package com.sainttx.auction;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.command.AuctionCommandHandler;
import com.sainttx.auction.command.BidCommand;
import com.sainttx.auction.structure.messages.GlobalChatHandler;
import com.sainttx.auction.structure.messages.HerochatHandler;
import com.sainttx.auction.util.AuctionUtil;
import com.sainttx.auction.util.TextUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class AuctionPlugin extends JavaPlugin implements Listener {

    /*
     * General
     */
    private static AuctionPlugin plugin;
    private static Economy economy;
    private MessageHandler messageHandler;

    /*
     * Offline item saving
     */
    private final File offlineFile = new File(getDataFolder(), "offline.yml");
    private YamlConfiguration offlineConfiguration;
    private static HashMap<String, ItemStack> offlinePlayers = new HashMap<String, ItemStack>();

    /**
     * Returns the Auction Plugin instance
     *
     * @return The auction plugin instance
     */
    public static AuctionPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Set the economy in the next tick so that all plugins are loaded
        Bukkit.getScheduler().runTask(this, new Runnable() {
            public void run() {
                economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
            }
        });

        // Setup
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig();
        TextUtil.load(this);

        // Load offline player items
        for (String string : offlineConfiguration.getKeys(false)) {
            ItemStack is = offlineConfiguration.getItemStack(string);
            offlinePlayers.put(string, is);
        }

        // Commands
        getCommand("auction").setExecutor(new AuctionCommandHandler());
        getCommand("bid").setExecutor(new BidCommand(this));
    }

    @Override
    public void onDisable() {
        AuctionManagerImpl.disable();
        TextUtil.save();

        // Logoff file
        try {
            if (!offlineFile.exists()) {
                offlineFile.createNewFile();
            }
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the Vault Economy provider
     *
     * @return Vault's economy hook
     */
    public static Economy getEconomy() {
        return economy;
    }

    /**
     * Saves a players auctioned item to file if the plugin was unable
     * to return it
     *
     * @param uuid The ID of a player
     * @param is   The item that the player auctioned
     */
    public void saveOfflinePlayer(UUID uuid, ItemStack is) {
        offlineConfiguration.set(uuid.toString(), is);
        offlinePlayers.put(uuid.toString(), is);

        try {
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * A helper method that initializes the chat handler
     */
    private void initializeChatHandler() {
        if (getServer().getPluginManager().isPluginEnabled("Herochat")
                && getConfig().getBoolean("integration.herochat.enable", false)) {
            this.messageHandler = new HerochatHandler(this);
            getLogger().info("Herochat was chosen as the chat channel");
        } else {
            this.messageHandler = new GlobalChatHandler();
            getLogger().info("GlobalChatHandler was chosen as the chat channel");
        }
    }

    /**
     * Loads the configuration
     */
    public void loadConfig() {
        saveDefaultConfig();
        initializeChatHandler();
        File names = new File(getDataFolder(), "items.yml");

        // Clear & set up auction broadcast times
        /* AuctionBlah.broadcastTimes.clear();
        for (String broadcastTime : getConfig().getStringList("general.broadcastTimes")) {
            try {
                Integer time = Integer.parseInt(broadcastTime);
                AuctionBlah.broadcastTimes.add(time);
            } catch (NumberFormatException ex) {
                getLogger().info("String \"" + broadcastTime + "\" is an invalid Integer, skipping");
            }
        } */

        // Save items file name
        if (!names.exists()) {
            saveResource("items.yml", false);
        }
        if (!offlineFile.exists()) {
            try {
                offlineFile.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        this.offlineConfiguration = YamlConfiguration.loadConfiguration(offlineFile);
    }

    @EventHandler
    /**
     * Responsible for giving the players back items that were unable to be
     * returned at a previous time
     */
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack saved = offlinePlayers.get(player.getUniqueId().toString());
        if (saved != null) {
            AuctionUtil.giveItem(player, saved, "saved-item-return");
            offlinePlayers.remove(player.getUniqueId().toString());
            offlineConfiguration.set(player.getUniqueId().toString(), null);

            try {
                offlineConfiguration.save(offlineFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    /**
     * Cancels a players command if they're auctioning
     */
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split(" ")[0];
        if (getConfig().getBoolean("general.blockCommands.ifAuctioning", false)
                && getConfig().isList("general.blockedCommands")
                && getConfig().getStringList("general.blockedCommands").contains(command.toLowerCase())) {
            Player player = event.getPlayer();
            Auction auction = AuctionsAPI.getAuctionManager().getCurrentAuction();

            if (AuctionsAPI.getAuctionManager().hasActiveAuction(player)) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("command-blocked-auctioning", player);
            } else if (getConfig().getBoolean("general.blockedCommands.ifQueued", false)
                    && AuctionsAPI.getAuctionManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("command-blocked-auction-queued", player);
            } else if (getConfig().getBoolean("general.blockCommands.ifTopBidder", false)
                    && auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("command-blocked-top-bidder", player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World target = event.getTo().getWorld();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && plugin.getConfig().isList("general.disabledWorlds")
                && plugin.getConfig().getStringList("general.disabledWorlds").contains(target.getName())) {
            if (AuctionsAPI.getAuctionManager().hasActiveAuction(player)
                    || AuctionsAPI.getAuctionManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("fail-teleport-world-disabled", player);
            } else {
                Auction auction = AuctionsAPI.getAuctionManager().getCurrentAuction();

                if (auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                    event.setCancelled(true);
                    AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("fail-teleport-world-disabled", player);
                }
            }
        }
    }
}
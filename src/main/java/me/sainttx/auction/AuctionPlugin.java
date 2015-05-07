package me.sainttx.auction;

import me.sainttx.auction.command.AuctionCommand;
import me.sainttx.auction.command.BidCommand;
import me.sainttx.auction.struct.MessageHandler;
import me.sainttx.auction.struct.messages.GlobalChatHandler;
import me.sainttx.auction.struct.messages.HerochatHandler;
import me.sainttx.auction.util.AuctionUtil;
import me.sainttx.auction.util.TextUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.Iterator;
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

    /*
     * Configuration
     */
    private FileConfiguration config;

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

        // message handling
        // message handling
        if ((!getServer().getPluginManager().isPluginEnabled("Herochat")
                && getConfig().getBoolean("settings.integrate-herochat", false))
                || !getConfig().getBoolean("settings.integrate-herochat", false)) {
            messageHandler = new GlobalChatHandler(this);
            getLogger().info("Using global chat handler. If you specific Herochat message handling, the Herochat plugin was not found.");
        } else {
            messageHandler = new HerochatHandler(this);
            getLogger().info("Using Herochat message handler");
        }

        // Load offline player items
        for (String string : offlineConfiguration.getKeys(false)) {
            ItemStack is = offlineConfiguration.getItemStack(string);
            offlinePlayers.put(string, is);
        }

        // Commands
        getCommand("auction").setExecutor(new AuctionCommand(this));
        getCommand("bid").setExecutor(new BidCommand(this));
    }

    @Override
    public void onDisable() {
        TextUtil.save();
        if (AuctionManager.getCurrentAuction() != null) {
            AuctionManager.getCurrentAuction().end(true);
        }

        // End all auctions that are queued
        for (Iterator<Auction> auctionIterator = AuctionManager.getAuctionManager().getAuctionQueue().iterator() ; auctionIterator.hasNext() ; ) {
            Auction auction = auctionIterator.next();
            auction.end(false);
            auctionIterator.remove();
        }

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
     * Returns the plugins configuration file
     *
     * @return The plugins configuration file
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Returns the message handler
     *
     * @return the message handler
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
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

    /**
     * Loads the configuration
     */
    public void loadConfig() {
        saveDefaultConfig();
        config = super.getConfig();
        getConfig().options().copyDefaults(true);
        File names = new File(getDataFolder(), "items.yml");

        // Clear & set up auction broadcast times
        Auction.broadcastTimes.clear();
        for (String broadcastTime : getConfig().getStringList("broadcast-times")) {
            try {
                Integer time = Integer.parseInt(broadcastTime);
                Auction.broadcastTimes.add(time);
            } catch (NumberFormatException ex) {
                getLogger().info("String \"" + broadcastTime + "\" is an invalid Integer, skipping");
            }
        }

        // Update the message handler
        if (getConfig().getBoolean("settings.integrate-herochat", false)) {
            messageHandler = new HerochatHandler(this);
            getLogger().info("Switched to Herochat message handling");
        } else if (!getConfig().getBoolean("settings.integrate-herochat", false)) {
            messageHandler = new GlobalChatHandler(this);
            getLogger().info("Switched to Global message handling");
        }

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
        if (getConfig().getBoolean("block-commands-when-auctioning", false)
                && getConfig().getStringList("blocked-commands").contains(event.getMessage().split(" ")[0].toLowerCase())) {
            Player player = event.getPlayer();
            Auction auction = AuctionManager.getCurrentAuction();

            if (AuctionManager.isAuctioningItem(player)) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage("command-blocked-auctioning", player);
            } else if (getConfig().getBoolean("block-commands-if-auction-queued", false) && AuctionManager.hasAuctionQueued(player)) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage("command-blocked-auction-queued", player);
            } else if (getConfig().getBoolean("block-commands-if-top-bidder", false)
                    && auction != null && auction.hasBids() && auction.getWinning().equals(player.getUniqueId())) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage("command-blocked-top-bidder", player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World target = event.getTo().getWorld();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && plugin.getConfig().isList("disabled-worlds")
                && plugin.getConfig().getStringList("disabled-worlds").contains(target.getName())) {
            if (AuctionManager.getAuctionManager().isAuctioningItem(player)
                    || AuctionManager.getAuctionManager().hasAuctionQueued(player)) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage("fail-teleport-world-disabled", player);
            } else {
                Auction auction = AuctionManager.getCurrentAuction();

                if (auction != null && auction.hasBids() && auction.getWinning().equals(player.getUniqueId())) {
                    event.setCancelled(true);
                    plugin.getMessageHandler().sendMessage("fail-teleport-world-disabled", player);
                }
            }
        }
    }
}
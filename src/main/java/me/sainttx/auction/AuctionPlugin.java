package me.sainttx.auction;

import me.sainttx.auction.command.CommandAuction;
import me.sainttx.auction.command.CommandBid;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
    public AuctionManager manager;
    private static Economy economy;

    /*
     * Offline item saving
     */
    private final File off = new File(getDataFolder(), "saveOfflinePlayer.yml");
    protected YamlConfiguration logoff;
    private static HashMap<String, ItemStack> loggedoff = new HashMap<String, ItemStack>();

    /*
     * Configuration
     */
    private YamlConfiguration config = this.getConfig();


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
        manager = AuctionManager.getAuctionManager();
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();

        // Setup
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig();
        TextUtil.load(this);

        // Load offline player items
        for (String string : logoff.getKeys(false)) {
            ItemStack is = logoff.getItemStack(string);
            loggedoff.put(string, is);
        }

        // Commands
        getCommand("auction").setExecutor(new CommandAuction(this));
        getCommand("bid").setExecutor(new CommandBid(this));
    }

    @Override
    public void onDisable() {
        TextUtil.save();
        if (AuctionManager.getCurrentAuction() != null) {
            AuctionManager.getCurrentAuction().end(true);
        }

        // Logoff file
        try {
            if (!off.exists()) {
                off.createNewFile();
            }
            logoff.save(off);
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
    public YamlConfiguration getConfig() {
        return config;
    }

    /**
     * Saves a players auctioned item to file if the plugin was unable
     * to return it
     * 
     * @param uuid  The ID of a player
     * @param is    The item that the player auctioned
     */
    public void saveOfflinePlayer(UUID uuid, ItemStack is) {
        logoff.set(uuid.toString(), is);
        loggedoff.put(uuid.toString(), is);

        try {
            logoff.save(off);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Loads the configuration
     */
    public void loadConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        File names = new File(getDataFolder(), "items.yml");

        if (!names.exists()) {
            saveResource("items.yml", false);
        }
        if (!off.exists()) {
            try {
                off.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        this.logoff = YamlConfiguration.loadConfiguration(off);
    }

    @EventHandler
    /**
     * Responsible for giving the players back items that were unable to be
     * returned at a previous time
     */
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack saved = loggedoff.get(player.getUniqueId().toString());
        if (saved != null) {
            AuctionUtil.giveItem(player, saved, "saved-item-return");
            loggedoff.remove(player.getUniqueId().toString());
            logoff.set(player.getUniqueId().toString(), null);

            try {
                logoff.save(off);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
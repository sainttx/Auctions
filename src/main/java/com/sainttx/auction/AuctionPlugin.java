package com.sainttx.auction;

import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.reward.Reward;
import com.sainttx.auction.command.AuctionCommandHandler;
import com.sainttx.auction.listener.PlayerListener;
import com.sainttx.auction.structure.messages.GlobalChatHandler;
import com.sainttx.auction.util.TextUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AuctionPlugin extends JavaPlugin {

    // Instance
    private static AuctionPlugin plugin;
    private Economy economy;

    // Offline items
    private final File offlineFile = new File(getDataFolder(), "offline.yml");
    private YamlConfiguration offlineConfiguration;
    private Map<UUID, Reward> offlineRewardCache = new HashMap<UUID, Reward>();

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
                try {
                    economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
                } catch (Throwable t) {
                    getLogger().log(Level.SEVERE, "failed to find an economy provider, disabling...", t);
                    getServer().getPluginManager().disablePlugin(AuctionPlugin.this);
                }
            }
        });

        // Setup
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        AuctionsAPI.getAuctionManager().setMessageHandler(new GlobalChatHandler());
        loadConfig();
        loadOfflineRewards();
        TextUtil.load(this);

        // Commands
        AuctionCommandHandler handler = new AuctionCommandHandler();
        getCommand("auction").setExecutor(handler);
        getCommand("bid").setExecutor(handler);
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
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Returns whether or not a time is an auction broadcast interval
     *
     * @param time the time in seconds left in an auction
     * @return true if the time is a broadcast time
     */
    public boolean isBroadcastTime(int time) {
        return getConfig().isList("general.broadcastTimes")
                && getConfig().getStringList("general.broadcastTimes").contains(Integer.toString(time));
    }

    /**
     * Gets a message from configuration
     *
     * @param path the path to the message
     * @return the message at the path
     */
    public String getMessage(String path) {
        return getConfig().getString(path);
    }

    /**
     * Saves a players auctioned reward to file if the plugin was unable
     * to return it
     *
     * @param uuid   The ID of a player
     * @param reward The reward that was auctioned
     */
    public void saveOfflinePlayer(UUID uuid, Reward reward) {
        offlineConfiguration.set(uuid.toString(), reward);
        offlineRewardCache.put(uuid, reward);

        try {
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Gets a stored reward for a UUID. Returns null if there is no reward for the id.
     *
     * @param uuid the uuid
     * @return the stored reward
     */
    public Reward getOfflineReward(UUID uuid) {
        return offlineRewardCache.get(uuid);
    }

    /**
     * Removes a reward that is stored for a UUID
     *
     * @param uuid the uuid
     */
    public void removeOfflineReward(UUID uuid) {
        offlineRewardCache.remove(uuid);
        offlineConfiguration.set(uuid.toString(), null);

        try {
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "failed to save offline configuration", ex);
        }
    }

    /**
     * Loads the configuration
     */
    public void loadConfig() {
        saveDefaultConfig();
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

    /*
     * A helper method that loads all offline rewards into memory
     */
    private void loadOfflineRewards() {
        for (String string : offlineConfiguration.getKeys(false)) {
            Reward reward = (Reward) offlineConfiguration.get(string);
            offlineRewardCache.put(UUID.fromString(string), reward);
        }
    }
}
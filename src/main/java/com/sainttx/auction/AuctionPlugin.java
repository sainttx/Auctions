package com.sainttx.auction;

import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.api.reward.Reward;
import com.sainttx.auction.command.AuctionCommandHandler;
import com.sainttx.auction.command.BidCommand;
import com.sainttx.auction.listener.PlayerListener;
import com.sainttx.auction.structure.messages.GlobalChatHandler;
import com.sainttx.auction.structure.messages.HerochatHandler;
import com.sainttx.auction.util.TextUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class AuctionPlugin extends JavaPlugin {

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
    private HashMap<String, Reward> offlinePlayers = new HashMap<String, Reward>();

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
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        loadConfig();
        TextUtil.load(this);
        AuctionsAPI.getAuctionManager().setMessageHandler(new GlobalChatHandler());

        // Load offline player items
        for (String string : offlineConfiguration.getKeys(false)) {
            Reward reward = (Reward) offlineConfiguration.get(string);
            offlinePlayers.put(string, reward);
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
     * Saves a players auctioned reward to file if the plugin was unable
     * to return it
     *
     * @param uuid   The ID of a player
     * @param reward The reward that was auctioned
     */
    public void saveOfflinePlayer(UUID uuid, Reward reward) {
        offlineConfiguration.set(uuid.toString(), reward);
        offlinePlayers.put(uuid.toString(), reward);

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
        return offlinePlayers.get(uuid.toString());
    }

    /**
     * Removes a reward that is stored for a UUID
     *
     * @param uuid the uuid
     */
    public void removeOfflineReward(UUID uuid) {
        offlinePlayers.remove(uuid.toString());
        offlineConfiguration.set(uuid.toString(), null);

        try {
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "failed to save offline configuration", ex);
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
}
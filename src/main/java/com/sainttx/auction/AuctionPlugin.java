package com.sainttx.auction;

import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandlerType;
import com.sainttx.auction.api.reward.Reward;
import com.sainttx.auction.command.AuctionCommandHandler;
import com.sainttx.auction.listener.PlayerListener;
import com.sainttx.auction.structure.messages.group.GlobalChatGroup;
import com.sainttx.auction.structure.messages.group.HerochatGroup;
import com.sainttx.auction.structure.messages.handler.ActionBarMessageHandler;
import com.sainttx.auction.structure.messages.handler.TextualMessageHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The auction plugin class
 */
public class AuctionPlugin extends JavaPlugin {

    // Instance
    private static AuctionPlugin plugin;
    private Economy economy;

    // Items file
    private YamlConfiguration itemsFile;

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
        saveDefaultConfig();

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

        // Message groups
        if (getConfig().getBoolean("integration.herochat.enable")) {
            AuctionsAPI.getAuctionManager().addMessageGroup(new HerochatGroup(this));
        }
        if (getConfig().getBoolean("chatSettings.groups.global")) {
            AuctionsAPI.getAuctionManager().addMessageGroup(new GlobalChatGroup());
        }

        // Message handler
        try {
            MessageHandlerType type = MessageHandlerType.valueOf(getMessage("chatSettings.handler"));
            switch (type) {
                case TEXT:
                    AuctionsAPI.getAuctionManager().setMessageHandler(new TextualMessageHandler());
                    getLogger().info("Message handler has been set to TEXT");
                    break;
                case ACTION_BAR:
                    AuctionsAPI.getAuctionManager().setMessageHandler(new ActionBarMessageHandler());
                    getLogger().info("Message handler has been set to ACTION_BAR");
                    break;
            }
        } catch (Throwable throwable) {
            getLogger().info("Failed to find a valid message handler, please make sure that your value" +
                    "for 'chatSettings.handler' is a valid message handler type.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        loadConfig();
        loadOfflineRewards();

        // Commands
        AuctionCommandHandler handler = new AuctionCommandHandler();
        getCommand("auction").setExecutor(handler);
        getCommand("sealedauction").setExecutor(handler);
        getCommand("bid").setExecutor(handler);
        getServer().getPluginManager().registerEvents(handler, this);
    }

    @Override
    public void onDisable() {
        plugin = null;
        AuctionManagerImpl.disable();

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
        if (!getConfig().isString(path)) {
            return path;
        }

        return getConfig().getString(path);
    }

    /**
     * Gets an items name
     *
     * @param item the item
     * @return the display name of the item
     */
    public String getItemName(ItemStack item) {
        short durability = item.getType().getMaxDurability() > 0 ? 0 : item.getDurability();
        String search = item.getType().toString() + "." + durability;
        String ret = itemsFile.getString(search);

        return ret == null ? getMaterialName(item.getType()) : ret;
    }

    /*
     * Converts a material to a string (ie. ARMOR_STAND = Armor Stand)
     */
    private String getMaterialName(Material material) {
        String[] split = material.toString().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();

        for (String str : split) {
            builder.append(str.substring(0, 1).toUpperCase() + str.substring(1) + " ");
        }

        return builder.toString().trim();
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
     * Formats a double to english
     *
     * @param d the double
     * @return the english string representation
     */
    public String formatDouble(double d) {
        NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(2);
        return format.format(d);
    }

    /**
     * Loads the configuration
     */
    public void loadConfig() {
        File names = new File(getDataFolder(), "items.yml");
        File namesFile = new File(plugin.getDataFolder(), "items.yml");

        // Save items file name
        if (!names.exists()) {
            saveResource("items.yml", false);
        }
        if (!namesFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        itemsFile = YamlConfiguration.loadConfiguration(namesFile);
    }

    /*
     * A helper method that loads all offline rewards into memory
     */
    private void loadOfflineRewards() {
        try {
            Class.forName("com.sainttx.auction.api.reward.ItemReward");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "failed to load offline rewards", t);
            return;
        }

        if (!offlineFile.exists()) {
            try {
                offlineFile.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        this.offlineConfiguration = YamlConfiguration.loadConfiguration(offlineFile);
        for (String string : offlineConfiguration.getKeys(false)) {
            Reward reward = (Reward) offlineConfiguration.get(string);
            offlineRewardCache.put(UUID.fromString(string), reward);
        }
    }
}
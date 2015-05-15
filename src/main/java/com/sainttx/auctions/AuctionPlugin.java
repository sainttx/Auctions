/*
 * Copyright (C) SainttX <http://sainttx.com>
 * Copyright (C) contributors
 *
 * This file is part of Auctions.
 *
 * Auctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Auctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Auctions.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sainttx.auctions;

import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandlerType;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.command.AuctionCommandHandler;
import com.sainttx.auctions.hook.PlaceholderAPIHook;
import com.sainttx.auctions.listener.PlayerListener;
import com.sainttx.auctions.structure.messages.group.GlobalChatGroup;
import com.sainttx.auctions.structure.messages.group.HerochatGroup;
import com.sainttx.auctions.structure.messages.handler.ActionBarMessageHandler;
import com.sainttx.auctions.structure.messages.handler.TextualMessageHandler;
import com.sainttx.auctions.util.ReflectionUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

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
        checkOutdatedConfig();

        // Set the economy in the next tick so that all plugins are loaded
        Bukkit.getScheduler().runTask(this, new Runnable() {
            public void run() {
                try {
                    economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
                } catch (Throwable t) {
                    getLogger().log(Level.SEVERE, "failed to find an economy provider, disabling...");
                    getServer().getPluginManager().disablePlugin(AuctionPlugin.this);
                }
            }
        });

        // Message groups
        if (getConfig().getBoolean("integration.herochat.enable")) {
            AuctionsAPI.getAuctionManager().addMessageGroup(new HerochatGroup(this));
            getLogger().info("Added Herochat recipient group to the list of broadcast listeners");
        }
        if (getConfig().getBoolean("chatSettings.groups.global")) {
            AuctionsAPI.getAuctionManager().addMessageGroup(new GlobalChatGroup());
            getLogger().info("Added global chat recipient group to the list of broadcast listeners");
        }

        // Register placeholders
        if (canRegisterPlaceholders()) {
            PlaceholderAPIHook.registerPlaceHolders(this);
            getLogger().info("Successfully registered PlaceholderAPI placeholders");
        } else {
            getLogger().info("PlaceholderAPI was not found, chat hooks have NOT been registered");
        }

        // Message handler
        try {
            MessageHandlerType type = MessageHandlerType.valueOf(getMessage("chatSettings.handler"));
            switch (type) {
                case ACTION_BAR:
                    String version = ReflectionUtil.getVersion();
                    if (version.startsWith("v1_8_R2") || version.startsWith("v1_8_R1")) {
                        AuctionsAPI.getAuctionManager().setMessageHandler(new ActionBarMessageHandler());
                        getLogger().info("Message handler has been set to ACTION_BAR");
                        break;
                    } else {
                        getLogger().info("Message handler type ACTION_BAR is unavailable for this Minecraft version. " +
                                "Defaulting to TEXT based message handling");
                    }
                case TEXT:
                    AuctionsAPI.getAuctionManager().setMessageHandler(new TextualMessageHandler());
                    getLogger().info("Message handler has been set to TEXT");
                    break;
            }
        } catch (Throwable throwable) {
            getLogger().info("Failed to find a valid message handler, please make sure that your value" +
                    "for 'chatSettings.handler' is a valid message handler type");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Enable plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (Exception ignored) {
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

    /*
     * A helper method that determines if placeholders can be registered
     */
    private boolean canRegisterPlaceholders() {
        try {
            return Class.forName("me.clip.placeholderapi.PlaceholderAPI") != null;
        } catch (Throwable throwable) {
            return false;
        }
    }

    /*
     * A helper method that determines if a plugins configuration is outdated
     * and prints out the missing pathways in the old config
     */
    @SuppressWarnings("deprecation")
    private void checkOutdatedConfig() {
        try {
            Configuration def = YamlConfiguration.loadConfiguration(getResource("config.yml"));
            int version = def.getInt("general.configurationVersion");

            if (getConfig().getInt("general.configurationVersion") < version) {
                File cfg = new File(getDataFolder(), "config.yml");
                YamlConfiguration curr = YamlConfiguration.loadConfiguration(cfg);

                if (def.getKeys(true).size() > curr.getKeys(true).size()) {
                    getLogger().info("Hey! Your configuration is out of date.");
                    getLogger().info("Here's what your config is missing:");

                    for (String key : def.getKeys(true)) {
                        if (!curr.contains(key)) {
                            getLogger().info("  - Missing path \"" + key + "\"");
                        }
                    }

                    getLogger().info("That's everything! You can check out the resource thread for the default values.");
                }
            }
        } catch (Exception ex) {
            getLogger().severe("Failed to determine if Auctions configuration is out of date");
        }
    }

    @Override
    public void onDisable() {
        AuctionManagerImpl.disable();

        // Logoff file
        try {
            if (!offlineFile.exists()) {
                offlineFile.getParentFile().mkdirs();
                offlineFile.createNewFile();
            }
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        plugin = null;
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
        format.setMinimumFractionDigits(0);
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
            Class.forName("com.sainttx.auctions.api.reward.ItemReward");
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
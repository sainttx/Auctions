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

import com.google.common.base.Joiner;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.Auctions;
import com.sainttx.auctions.api.MessageFactory;
import com.sainttx.auctions.api.Settings;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.command.AuctionCommands;
import com.sainttx.auctions.command.module.AuctionsModule;
import com.sainttx.auctions.listener.AuctionListener;
import com.sainttx.auctions.listener.PlayerListener;
import com.sainttx.auctions.structure.messages.group.GlobalChatGroup;
import com.sainttx.auctions.structure.messages.group.HerochatGroup;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Intake;
import com.sk89q.intake.InvalidUsageException;
import com.sk89q.intake.InvocationCommandException;
import com.sk89q.intake.argument.Namespace;
import com.sk89q.intake.dispatcher.Dispatcher;
import com.sk89q.intake.fluent.CommandGraph;
import com.sk89q.intake.parametric.Injector;
import com.sk89q.intake.parametric.ParametricBuilder;
import com.sk89q.intake.parametric.provider.PrimitivesModule;
import com.sk89q.intake.util.auth.AuthorizationException;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * The auction plugin class
 */
public class AuctionPluginImpl extends JavaPlugin implements AuctionPlugin {

    // Instance
    private AuctionManager manager;
    private Economy economy;
    private Settings settings;

    // Items file
    private YamlConfiguration itemsFile;

    // Offline items
    private final File offlineFile = new File(getDataFolder(), "offline.yml");
    private YamlConfiguration offlineConfiguration;
    private Map<UUID, Reward> offlineRewardCache = new HashMap<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Dispatcher dispatcher;
    private MessageFactory messageFactory;

    // All valid commands
    private Map<String, String> commands = new TreeMap<String, String>() {{
        put("start", "auctions.command.start");
        put("bid", "auctions.command.bid");
        put("info", "auctions.command.info");
        put("end", "auctions.command.end");
        put("cancel", "auctions.command.cancel");
        put("impound", "auctions.command.impound");
        put("ignore", "auctions.command.ignore");
        put("spam", "auctions.command.spam");
        put("queue", "auctions.command.queue");
        put("toggle", "auctions.command.toggle");
        put("reload", "auctions.command.reload");
    }};

    @Override
    public void onEnable() {
        saveDefaultConfig();
        checkOutdatedConfig();
        settings = new LocalSettings(this);
        messageFactory = new SimpleMessageFactory(this);

        // Set the economy in the next tick so that all plugins are loaded
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "failed to find an economy provider, disabling...");
                getServer().getPluginManager().disablePlugin(AuctionPluginImpl.this);
            }
        });

        // Create manager instance
        this.manager = new AuctionManagerImpl();
        Auctions.setManager(manager);

        // Message groups
        if (getConfig().getBoolean("integration.herochat.enable")) {
            manager.addMessageGroup(new HerochatGroup(this));
            getLogger().info("Added Herochat recipient group to the list of broadcast listeners");
        }
        if (getConfig().getBoolean("chatSettings.groups.global")) {
            manager.addMessageGroup(new GlobalChatGroup());
            getLogger().info("Added global chat recipient group to the list of broadcast listeners");
        }

        // Enable plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (Exception ignored) {
        }

        Injector injector = Intake.createInjector();
        injector.install(new AuctionsModule());
        injector.install(new PrimitivesModule());
        ParametricBuilder builder = new ParametricBuilder(injector);

        // The authorizer will test whether the command sender has permission.
        builder.setAuthorizer((locals, permission) -> {
            CommandSender sender = locals.get(CommandSender.class);
            return sender != null && sender.hasPermission(permission);
        });

        dispatcher = new CommandGraph()
                .builder(builder)
                .commands()
                .group("auction")
                .registerMethods(new AuctionCommands(this))
                .parent()
                .graph()
                .getDispatcher();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new AuctionListener(this), this);
        loadConfig();
        loadOfflineRewards();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadConfig();
    }

    // Loads the items.yml file
    private void loadConfig() {
        File itemsFile = new File(getDataFolder(), "items.yml");

        // Save items file name
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }

        this.itemsFile = YamlConfiguration.loadConfiguration(itemsFile);
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

                    def.getKeys(true).stream()
                            .filter(key -> !curr.contains(key))
                            .forEach(key -> getLogger().info("  - Missing path \"" + key + "\""));

                    getLogger().info("That's everything! You can check out the resource thread for the default values.");
                }
            }
        } catch (Exception ex) {
            getLogger().severe("Failed to determine if Auctions configuration is out of date");
        }
    }

    @Override
    public void onDisable() {
        ((AuctionManagerImpl) manager).disable();

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

        Auctions.setManager(null);
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Reconstruct the full command message.
        final String message = command.getName() + " " + StringUtils.join(args, " ");
        final Namespace namespace = new Namespace();

        // The CommandSender is made always available.
        namespace.put(AuctionPlugin.class, this);
        namespace.put(CommandSender.class, sender);

        // Used to determine command prefix.
        final boolean isConsole = sender == Bukkit.getConsoleSender();

        // Execute the command. The dispatcher runs asynchronously, allowing parameter bindings
        // to be resolved without blocking the server thread. The command runs synchronously.
        executorService.execute(() -> {
            try {
                // Execute dispatcher with the fully reconstructed command message.
                dispatcher.call(message, namespace, Collections.emptyList());
            } catch (InvalidUsageException e) {
                // Invalid command usage should not be harmful. Print something friendly.
                if (e.isFullHelpSuggested()) {
                    StringBuilder builder = new StringBuilder(ChatColor.RED + "Subcommands: ");
                    Joiner joiner = Joiner.on(", ");

                    // Join all sub-commands that the player has permission for
                    String str = joiner.join(
                            commands.entrySet().stream()
                                    .filter(entry -> sender.hasPermission(entry.getValue()))
                                    .map(Map.Entry::getKey)
                                    .collect(Collectors.toList())
                    );

                    builder.append(str);
                    sender.sendMessage(builder.toString());
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: " + e.getSimpleUsageString(isConsole ? "" : "/"));
                }
                sender.sendMessage(ChatColor.RED + e.getMessage());
            } catch (AuthorizationException e) {
                // Print friendly message in case of permission failure.
                sender.sendMessage(ChatColor.RED + "Permission denied.");
            } catch (CommandException | InvocationCommandException e) {
                // Everything else is unexpected and should be considered an error.
                throw new RuntimeException(e);
            }
        });

        return true;
    }

    @Override
    public AuctionManager getManager() {
        return manager;
    }

    @Override
    public Economy getEconomy() {
        return economy;
    }

    @Override
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
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

    @Override
    public void saveOfflinePlayer(UUID uuid, Reward reward) {
        offlineConfiguration.set(uuid.toString(), reward);
        offlineRewardCache.put(uuid, reward);

        try {
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Reward getOfflineReward(UUID uuid) {
        return offlineRewardCache.get(uuid);
    }

    @Override
    public void removeOfflineReward(UUID uuid) {
        offlineRewardCache.remove(uuid);
        offlineConfiguration.set(uuid.toString(), null);

        try {
            offlineConfiguration.save(offlineFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "failed to save offline configuration", ex);
        }
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
            Object obj = offlineConfiguration.get(string);
            Reward reward;

            if (obj instanceof Reward) {
                reward = (Reward) offlineConfiguration.get(string);
            } else if (obj instanceof ItemStack) {
                reward = new ItemReward(this, (ItemStack) obj);
            } else {
                getLogger().info("Cannot load offline reward for player with UUID \""
                        + string + "\", unknown reward type \"" + obj.getClass().getName() + "\"");
                continue;
            }

            offlineRewardCache.put(UUID.fromString(string), reward);
        }
    }
}
package me.sainttx.auction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Auction extends JavaPlugin implements Listener {

    private static Auction auction;
    private static Messages messages;
    private static AuctionManager manager;
    public static Economy economy = null;

    private File off = new File(getDataFolder(), "save.yml");
    private YamlConfiguration logoff;
    private static FileConfiguration config;

    private static HashMap<String, ItemStack> loggedoff = new HashMap<String, ItemStack>();

    @Override
    public void onEnable() {
        auction = this;
        saveDefaultConfig();
        loadConfig();
        setupEconomy();
        loadSaved();
        getCommand("auction").setExecutor(this);
        getCommand("bid").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        manager = AuctionManager.getAuctionManager();
    }

    @Override
    public void onDisable() {
        AuctionManager.disable();
        try {
            if (!off.exists()) {
                off.createNewFile();
            }
            logoff.save(off);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Auction getPlugin() {
        return auction;
    }

    public void reload() {
        reloadConfig();
        config = getConfig();
        messages = Messages.getMessager();
        manager = AuctionManager.getAuctionManager();
    }

    public static FileConfiguration getConfiguration() {
        return config;
    }

    public static Messages getMessager() {
        return messages;
    }
    public static AuctionManager getAuctionManager() {
        return manager;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public YamlConfiguration getLogOff() {
        return logoff;
    }

    public void save(UUID uuid, ItemStack is) { 
        logoff.set(uuid.toString(), is);
        loggedoff.put(uuid.toString(), is);
        try {
            logoff.save(off);
        } catch (IOException e) {
        }
    }

    public void loadSaved() {
        for (String string : logoff.getKeys(false)) {
            ItemStack is = logoff.getItemStack(string);
            loggedoff.put(string, is);
        }
    }

    private void loadConfig() {
        config = getConfig();
        File names = new File(getDataFolder(), "items.yml");
        if (!names.exists()) {
            saveResource("items.yml", false);
        }
        if (!off.exists()) {
            try {
                off.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        this.logoff = YamlConfiguration.loadConfiguration(off);
        messages = Messages.getMessager();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack saved = loggedoff.get(player.getUniqueId().toString());
        if (saved != null) {
            giveItem(player, saved, "saved-item-return");
            loggedoff.remove(player.getUniqueId().toString());
            logoff.set(player.getUniqueId().toString(), null);
            try {
                logoff.save(off);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void giveItem(Player player, ItemStack itemstack, String... messageentry) {
        World world = player.getWorld();
        boolean dropped = false;
        int maxsize = itemstack.getMaxStackSize();
        int amount = itemstack.getAmount();
        int stacks = amount / maxsize;
        int remaining = amount % maxsize;
        ItemStack[] split = new ItemStack[1];
        if (amount > maxsize) {
            split = new ItemStack[stacks + (remaining > 0 ? 1 : 0)];
            // ie. 70 stack can only be 64
            for (int i = 0 ; i < stacks ; i++) {
                ItemStack maxStackSize = itemstack.clone();
                maxStackSize.setAmount(maxsize);
                split[i] = maxStackSize;
            }
            if (remaining > 0) {
                ItemStack remainder = itemstack.clone();
                remainder.setAmount(remaining);
                split[stacks] = remainder;
            }
        } else {
            split[0] = itemstack;
        }
        
        for (ItemStack item : split) {            
            if (item != null) {
                // Check their inventory space
                if (hasSpace(player.getInventory(), item)) {
                    player.getInventory().addItem(item);
                } else {
                    world.dropItem(player.getLocation(), item);
                    dropped = true;
                }
            }
        }
        if (messageentry.length == 1) {
            messages.sendText((CommandSender) player, messageentry[0], true);
        } 
        if (dropped) {
            messages.sendText((CommandSender) player, "items-no-space", true);
        } 
    }
    

    private boolean hasSpace(Inventory inventory, ItemStack itemstack) {
        int total = 0;
        for (ItemStack is : inventory.getContents()) {
            if (is == null) {
                total += itemstack.getMaxStackSize();
            } else if (is.isSimilar(itemstack)) {
                total += itemstack.getMaxStackSize() - is.getAmount();
            }
        }
        return total >= itemstack.getAmount();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String username = sender.getName();
        String cmdLabel = cmd.getLabel().toLowerCase();
        try {
            if (cmdLabel.equals("bid") && sender instanceof Player) {
                Player player = (Player) sender;
                if (!sender.hasPermission("auction.bid")) {
                    messages.sendText(sender, "insufficient-permissions", true);
                    return false;
                }
                if (args.length == 0 && getConfig().getBoolean("allow-autobid")) {
                    IAuction auction = manager.getAuctionInWorld(player);
                    if (auction == null) {
                        messages.sendText(sender, "fail-bid-no-auction", true);
                        return false;
                    }
                    manager.bid(player, (int) (auction.getTopBid() + auction.getIncrement())); 
                } else if (args.length == 1) {
                    manager.bid(player, Integer.parseInt(args[0]));
                } else {
                    messages.sendText(sender, "fail-bid-syntax", true);
                }
                return false;
            }
            
            if (args.length == 0) {
                messages.sendMenu(sender);
            } else {
                String subCommand = args[0].toLowerCase();
                
                if (!sender.hasPermission("auction." + subCommand)) {
                    messages.sendText(sender, "insufficient-permissions", true);
                    return false;
                }
                if (subCommand.equals("reload")) {
                    messages.sendText(sender, "reload", true);
                    reloadConfig();
                    loadConfig();
                } else if (subCommand.equals("disable")) {
                    if (!manager.isDisabled()) {
                        manager.setDisabled(true);
                        messages.messageListeningAll(messages.getMessageFile().getString("broadcast-disable"));
                    } else {
                        messages.sendText(sender, "already-disabled", true);
                    }
                } else if (subCommand.equals("enable")) {
                    if (manager.isDisabled()) {
                        manager.setDisabled(false);
                        messages.messageListeningAll(messages.getMessageFile().getString("broadcast-enable"));
                    } else {
                        messages.sendText(sender, "already-enabled", true);
                    }
                } else {
                    if (sender instanceof ConsoleCommandSender) {
                        getLogger().info("Console can only use reload, disable, and enable");
                        return false;
                    }
                }

                Player player = (Player) sender;

                if (subCommand.equals("start")) {
                    if (!messages.isIgnoring(username)) {
                        if (player.getGameMode() == GameMode.CREATIVE && !getConfig().getBoolean("allow-creative") && !player.hasPermission("auction.creative")) {
                            messages.sendText(sender, "fail-start-creative", true);
                            return false;
                        }
                        manager.startAuction(player, args);
                    } else {
                        messages.sendText(sender, "fail-start-ignoring", true);
                    }
                } else if (subCommand.equals("bid")) {
                    if (args.length == 2) {
                        try  {
                            manager.bid(player, Integer.parseInt(args[1])); 
                        } catch (NumberFormatException ex1) {
                            messages.sendText(sender, "fail-bid-number", true);
                        }
                    } else {
                        messages.sendText(sender, "fail-bid-syntax", true);
                    }
                } else if (subCommand.equals("info")) {
                    manager.sendInfo(player);
                } else if (subCommand.equals("end")) {
                    manager.end(player);
                } else if (subCommand.equals("ignore") || subCommand.equals("quiet")) {
                    if (!messages.isIgnoring(username)) {
                        messages.sendText(sender, "ignoring-on", true);
                        messages.addIgnoring(username);
                    } else {
                        messages.sendText(sender, "ignoring-off", true);
                        messages.removeIgnoring(username);
                    }
                }
            }
        } catch (NumberFormatException ex1) {
            messages.sendText(sender, "fail-bid-number", true);
        }
        return false;
    }

    public static boolean getBoolean(String configpath) {
        return config.getBoolean(configpath);
    }

    public static int getInt(String configpath) {
        return config.getInt(configpath);
    }

    public static String getString(String configpath) {
        return config.getString(configpath);
    }
}
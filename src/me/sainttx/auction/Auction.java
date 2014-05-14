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
        //messages = new Messages();
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
        ItemStack[] itemstacksplit = splitStack(itemstack);
        World world = player.getWorld();
        boolean dropped = false;
        for (ItemStack item : itemstacksplit) {
            if (item != null) {
                // Check their inventory space
                if (hasSpace(player.getInventory(), itemstack)) {
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

    private ItemStack[] splitStack(ItemStack itemstack) {
        ItemStack copy = itemstack.clone();
        int maxsize = copy.getMaxStackSize();
        int amount = copy.getAmount();
        int arraysize = (int) Math.ceil(amount / maxsize);

        ItemStack[] itemstackarray = new ItemStack[arraysize == 0 ? 1 : arraysize];
        if (amount > maxsize) {
            for (int i = 0 ; i < itemstackarray.length ; i++) {
                if (amount <= maxsize) {
                    // last item stack = amount
                    copy.setAmount(amount);
                    itemstackarray[i] = copy.clone();
                    break;
                }
                copy.setAmount(maxsize);
                itemstackarray[i] = copy.clone();
            }
        } else {
            itemstackarray[0] = copy;
        }
        return itemstackarray;
    }

    // This works
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


    //TODO remove any auction != null

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String username = sender.getName();
        if (args.length == 0 && !cmd.getLabel().toLowerCase().equals("bid")) {
            messages.sendMenu(sender);
        } else {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (cmd.getLabel().toLowerCase().equals("bid")) {
                    if (!player.hasPermission("auction.bid") && !player.isOp()) {
                        messages.sendText(sender, "insufficient-permissions", true);
                        return false;
                    }
                    if (args.length == 1) {
                        try {
                            manager.bid(player, Integer.parseInt(args[0]));
                        } catch (NumberFormatException ex1) {
                            messages.sendText(sender, "fail-bid-number", true);
                        }
                    } else {
                        messages.sendText(sender, "fail-bid-syntax", true);
                    }
                    return true;
                }
                String arg1 = args[0].toLowerCase();
                if (!player.hasPermission("auction." + arg1) && !player.isOp()) {
                    messages.sendText(sender, "insufficient-permissions", true);
                    return false;
                }
                if (arg1.equals("start")) {
                    if (!messages.isIgnoring(username)) {
                        if (player.getGameMode() == GameMode.CREATIVE && !getConfig().getBoolean("allow-creative") && !player.hasPermission("auction.creative")) {
                            messages.sendText(sender, "fail-start-creative", true);
                            return false;
                        }
                        manager.startAuction(player, args);
                    } else {
                        messages.sendText(sender, "fail-start-ignoring", true);
                    }
                } else if (arg1.equals("end")) {
                    manager.end(player);
                } else if (arg1.equals("info")) {
                    manager.sendInfo(player);
                } else if (arg1.equals("bid")) {
                    if (args.length == 2) {
                        try  {
                            manager.bid(player, Integer.parseInt(args[1])); //TODO
                        } catch (NumberFormatException ex1) {
                            messages.sendText(sender, "fail-bid-number", true);
                        }
                    } else {
                        messages.sendText(sender, "fail-bid-syntax", true);
                    }
                } else if (arg1.equals("quiet") || arg1.equals("ignore")) {
                    if (!messages.isIgnoring(username)) {
                        messages.sendText(sender, "ignoring-on", true);
                        messages.addIgnoring(username);
                    } else {
                        messages.sendText(sender, "ignoring-off", true);
                        messages.removeIgnoring(username);
                    }
                } else if (!arg1.equals("reload")) {
                    messages.sendMenu(player);
                    return false;
                }
            }
            if (args[0].toLowerCase().equals("reload")) {
                if (sender.hasPermission("auction.reload")) {
                    messages.sendText(sender, "reload", true);
                    reloadConfig();
                    loadConfig();
                } else {
                    messages.sendText(sender, "insufficient-permissions", true);
                }
            } else {
                if (sender instanceof ConsoleCommandSender) {
                    System.out.print("Console can't use this command.");
                    return false;
                } 
            }
        }
        return false;
    }
}
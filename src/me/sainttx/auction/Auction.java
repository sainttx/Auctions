package me.sainttx.auction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Auction extends JavaPlugin implements Listener {

    private static Auction auction;
    private Messages messages;
    private AuctionManager manager;
    private AuctionUtil autil;
    
    public static Economy economy = null;

    private File off = new File(getDataFolder(), "save.yml");
    private YamlConfiguration logoff;

    private static HashMap<String, ItemStack> loggedoff = new HashMap<String, ItemStack>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        auction = this;
        manager = AuctionManager.getAuctionManager();
        autil = new AuctionUtil();
        
        loadConfig();
        loadSaved();
        
        setupEconomy();
        
        getCommand("auction");
        getCommand("bid");
    }

    @Override
    public void onDisable() {
        manager.endAllAuctions();
        try {
            if (!off.exists()) {
                off.createNewFile();
            }
            logoff.save(off);
            saveConfig();
            messages.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Auction getPlugin() {
        return auction;
    }

    public void reload() {
        reloadConfig();
        messages = Messages.getMessager();
        manager = AuctionManager.getAuctionManager();
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
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
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
            autil.giveItem(player, saved, "saved-item-return");
            loggedoff.remove(player.getUniqueId().toString());
            logoff.set(player.getUniqueId().toString(), null);
            try {
                logoff.save(off);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String username = sender.getName();
        String cmdLabel = cmd.getLabel().toLowerCase();
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
                manager.prepareBid(player, (int) (auction.getTopBid() + auction.getIncrement())); 
            } else if (args.length == 1) {
                manager.prepareBid(player, args[0]);
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
                messages.reload();
            } else if (subCommand.equals("disable")) {
                if (!manager.areAuctionsDisabled()) {
                    manager.setDisabled(true);
                    messages.messageListeningAll(messages.getMessageFile().getString("broadcast-disable"));
                } else {
                    messages.sendText(sender, "already-disabled", true);
                }
            } else if (subCommand.equals("enable")) {
                if (manager.areAuctionsDisabled()) {
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
                    manager.prepareAuction(player, args);
                } else {
                    messages.sendText(sender, "fail-start-ignoring", true);
                }
            } else if (subCommand.equals("bid")) {
                if (args.length == 2) {
                    manager.prepareBid(player, args[1]); 
                } else {
                    messages.sendText(sender, "fail-bid-syntax", true);
                }
            } else if (subCommand.equals("info")) {
                manager.sendAuctionInfo(player);
            } else if (subCommand.equals("end")) {
                manager.end(player);
            } else if (subCommand.equals("ignore") || subCommand.equals("quiet")) {
                if (!messages.isIgnoring(username)) {
                    messages.addIgnoring(username);
                    messages.sendText(sender, "ignoring-on", true);
                } else {
                    messages.removeIgnoring(username);
                    messages.sendText(sender, "ignoring-off", true);
                }
            }
        }
        return false;
    }

    public boolean isPerWorldAuctions() {
        return getConfig().getBoolean("per-world-auctions", true);
    }

    public boolean getTellOtherWorldsStart() {
        return getConfig().getBoolean("tell-other-worlds-auction-start", false);
    }

    public boolean isLoggingAuctionsAllowed() {
        return getConfig().getBoolean("log-auctions", false);
    }

    public boolean isAuctionEndingAllowed() {
        return getConfig().getBoolean("allow-end", false);
    }

    public boolean isAutowinAllowed() {
        return getConfig().getBoolean("allow-autowin", false);
    }

    public boolean isAutobidAllowed() {
        return getConfig().getBoolean("allow-autobid", false);
    }
    public boolean isCreativeAllowed() {
        return getConfig().getBoolean("allow-creative", false);
    }

    public boolean isAntiSnipingAllowed() {
        return getConfig().getBoolean("anti-snipe", false);
    }

    public int getAntiSnipingPeriod() {
        return getConfig().getInt("anti-snipe-period", 3);
    }

    public double getIncrement() {
        return getConfig().getDouble("minimum-bid-increment", 1D);
    }
    
    public int getTimeToAdd() {
        return getConfig().getInt("anti-snipe-add-seconds", 5);
    }

    public int getAuctionStartTime() {
        return getConfig().getInt("auction-time", 30);
    }

    public int getAuctionStartFee() {
        return getConfig().getInt("auction-start-fee", 0);
    }

    public int getAuctionTaxPercentage() {
        return getConfig().getInt("auction-tax-percentage", 0);
    }

    public int getMinimumStartingPrice() {
        return getConfig().getInt("min-start-price", 0);
    }

    public int getMaximumStartingPrice() {
        return getConfig().getInt("max-start-price", Integer.MAX_VALUE);
    }
}
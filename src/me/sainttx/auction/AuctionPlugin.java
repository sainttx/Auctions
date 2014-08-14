package me.sainttx.auction;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import lombok.Getter;
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

public class AuctionPlugin extends JavaPlugin implements Listener {

    private static @Getter AuctionPlugin plugin;
    public AuctionManager manager;

    public static Economy economy = null;

    private final File off = new File(getDataFolder(), "save.yml");
    protected YamlConfiguration logoff;

    private static HashMap<String, ItemStack> loggedoff = new HashMap<String, ItemStack>();
    
    /*
     * Config
     */
    protected @Getter boolean logging;
    protected @Getter boolean allowEnding;
    protected @Getter boolean allowAutowin;
    protected @Getter boolean allowAutobid;
    protected @Getter boolean allowCreative;
    
    protected @Getter int     defaultAuctionTime;
    protected @Getter int     taxPercentage;
    
    protected @Getter double  startFee;
    protected @Getter double  minBidIncrement;
    protected @Getter double  minimumStartPrice;
    protected @Getter double  maxiumumStartPrice;
    
    public AuctionPlugin() {
        logging = getConfig().getBoolean("log-auctions", false);
        allowEnding = getConfig().getBoolean("allow-end", false);
        allowAutowin = getConfig().getBoolean("allow-autowin", false);
        allowAutobid = getConfig().getBoolean("allow-autobid", false);
        allowCreative = getConfig().getBoolean("allow-creative", false);
        
        defaultAuctionTime =  getConfig().getInt("auction-time", 30);
        taxPercentage = getConfig().getInt("auction-tax-percentage", 0);
        
        startFee = getConfig().getDouble("auction-start-fee", 0);
        minBidIncrement = getConfig().getDouble("minimum-bid-increment", 1D);
        minimumStartPrice = getConfig().getDouble("min-start-price", 0);;
        maxiumumStartPrice = getConfig().getDouble("max-start-price", Integer.MAX_VALUE);
    }

    @Override
    public void onEnable() {
        plugin = this;
        economy = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
        
        // Setup
        getServer().getPluginManager().registerEvents(this, this);
        manager = AuctionManager.getAuctionManager();
        loadConfig();
        loadSaved();

        // Commands
        getCommand("auction").setExecutor(new CommandAuction(this));
        getCommand("bid").setExecutor(new CommandBid(this));
    }

    @Override
    public void onDisable() {
        AuctionManager.getCurrentAuction().end();

        try {
            if (!off.exists()) {
                off.createNewFile();
            }
            
            logoff.save(off);
            saveConfig();
            Messages.getMessager().save();
        } catch (IOException e) { }
    }
    
    public void reload() {
        reloadConfig();
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
        } catch (IOException e) { }
    }

    public void loadSaved() {
        for (String string : logoff.getKeys(false)) {
            ItemStack is = logoff.getItemStack(string);
            loggedoff.put(string, is);
        }
    }

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
        this.logoff = YamlConfiguration.loadConfiguration(off);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack saved = loggedoff.get(player.getUniqueId().toString());
        if (saved != null) {
            new AuctionUtil().giveItem(player, saved, "saved-item-return");
            loggedoff.remove(player.getUniqueId().toString());
            logoff.set(player.getUniqueId().toString(), null);
            try {
                logoff.save(off);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
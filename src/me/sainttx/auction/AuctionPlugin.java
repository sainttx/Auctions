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

    // General
    private static @Getter AuctionPlugin plugin;
    public AuctionManager manager;
    public static Economy economy = null;

    // Offline item saving
    private final File off = new File(getDataFolder(), "save.yml");
    protected YamlConfiguration logoff;
    private static @Getter HashMap<String, ItemStack> loggedoff = new HashMap<String, ItemStack>();

    // Configuration
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

    /**
     * Instantiates the Auction plugin
     */
    public AuctionPlugin() {
        logging             = getConfig().getBoolean("log-auctions",    false);
        allowEnding         = getConfig().getBoolean("allow-end",       false);
        allowAutowin        = getConfig().getBoolean("allow-autowin",   false);
        allowAutobid        = getConfig().getBoolean("allow-autobid",   false);
        allowCreative       = getConfig().getBoolean("allow-creative",  false);

        defaultAuctionTime  = getConfig().getInt("auction-time",           30);
        taxPercentage       = getConfig().getInt("auction-tax-percentage", 0);

        startFee            = getConfig().getDouble("auction-start-fee",     0);
        minBidIncrement     = getConfig().getDouble("minimum-bid-increment", 1D);
        minimumStartPrice   = getConfig().getDouble("min-start-price",       0);
        maxiumumStartPrice  = getConfig().getDouble("max-start-price",       Integer.MAX_VALUE);
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
        createFile(off);
        saveFile(logoff, off);
        saveConfig();
        Messages.getMessager().save();
        AuctionManager.getCurrentAuction().end();
    }

    /**
     * Reloads the configuration
     */
    public void reload() {
        reloadConfig();
    }

    /**
     * Saves a players auctioned item to file if the plugin was unable
     * to return it
     * 
     * @param uuid  The ID of a player
     * @param is    The item that the player auctioned
     */
    public void save(UUID uuid, ItemStack is) { 
        logoff.set(uuid.toString(), is);
        loggedoff.put(uuid.toString(), is);

        saveFile(logoff, off);
    }

    /**
     * Loads the file which contains all information about saved items
     */
    public void loadSaved() {
        for (String string : logoff.getKeys(false)) {
            ItemStack is = logoff.getItemStack(string);
            loggedoff.put(string, is);
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
            this.createFile(off);
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

            saveFile(logoff, off);
        }
    }

    /**
     * Saves a YML file to disk
     */
    public void saveFile(YamlConfiguration yml, File f) {
        try {
            yml.save(f);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a file on disk
     */
    public void createFile(File f) {
        try {
            f.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
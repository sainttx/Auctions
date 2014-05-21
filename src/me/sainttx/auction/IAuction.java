package me.sainttx.auction;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IAuction {
    private Auction plugin;
    private boolean taxable = false;
    private UUID owner;
    private String worldName;
    private int numItems; // amount of items being auctioned
    private double autoWin;
    private int taskID;
    private int timeLeft;
    private double increment;
    private ItemStack item;

    private UUID winning;
    private double topBid;

    private final int[] times = {45, 30, 10, 3, 2, 1};

    public IAuction(Auction plugin, Player player, int numItems, int startingAmount, int autoWin)
            throws InsufficientItemsException, EmptyHandException, UnsupportedItemException {
        this.plugin = plugin;
        this.numItems = numItems;
        topBid = startingAmount;
        owner = player.getUniqueId();
        item = player.getItemInHand().clone();
        item.setAmount(numItems);
        increment = plugin.getConfig().getInt("minimum-bid-increment");
        worldName = player.getWorld().getName();

        if ((autoWin < topBid + increment) && autoWin != -1) {
            this.autoWin = topBid + increment;
        } else {
            this.autoWin = autoWin;
        }
        try {
            timeLeft = Integer.parseInt(plugin.getConfig().getString("auction-time")); // could throw on invalid
        } catch (NumberFormatException ex1) {
            plugin.getLogger().severe("Config value auction-time is an invalid Integer");
            timeLeft = 30;
        }

        if (item.getType() == Material.AIR) {
            throw new EmptyHandException();
        } 
        if (item.getType() == Material.FIREWORK || item.getType() == Material.FIREWORK_CHARGE || AuctionManager.getBannedMaterials().contains(item.getType())) {
            throw new UnsupportedItemException();
        }
        if (searchInventory(player)) { // Checks if they have enough of the item
            player.getInventory().removeItem(item);
        } else {
            throw new InsufficientItemsException();
        }
    }

    public UUID getOwner() {
        return owner;
    }

    public UUID getWinning() {
        return winning;
    }

    public void setWinning(UUID winning) {
        this.winning = winning;
    }

    public double getTopBid() {
        return topBid;
    }

    public void setTopBid(int topBid) {
        this.topBid = topBid;
    }

    public int getNumItems() {
        return numItems;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getIncrement() {
        return increment;
    }

    public double getAutoWin() {
        return autoWin;
    }

    public double getCurrentTax() {
        int tax = plugin.getConfig().getInt("auction-tax-percentage");
        return (topBid * tax) / 100;
    }

    public boolean hasBids() {
        return winning != null;
    }

    public String getTime() {
        return getFormattedTime();
    }
    
    public void addTime(int time) {
        timeLeft += time;
    }
    
    public int getTimeLeft() {
        return timeLeft;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }

    public void start() {
        if (Auction.getConfiguration().getBoolean("tell-other-worlds-auction-start")) {
            if (Auction.getConfiguration().getBoolean("per-world-auctions")) {
                Messages.getMessager().messageListeningAllOther(this, "auction-in-other-world", true);
            }
            Messages.getMessager().messageListeningAll(this, "auction-start", true, true);
            Messages.getMessager().messageListeningAll(this, "auction-start-price", true, false);
        } else {
            Messages.getMessager().messageListeningAll(this, "auction-start", true, true); 
            Messages.getMessager().messageListeningAll(this, "auction-start-price", true, true);
        }
        if (autoWin != -1) {
            Messages.getMessager().messageListeningAll(this, "auction-start-autowin", true, true);
        }
        final IAuction auc = this;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    end();
                } else {
                    --timeLeft;
                    for (int i : times) {
                        if (i == timeLeft) {
                            Messages.getMessager().messageListeningAll(auc, "auction-timer", true, true);
                            //plugin.messageListening(plugin.getMessageFormatted("auction-timer"));
                            break;
                        }
                    }
                }
            }
        };
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 0L, 20L);
    }

    public boolean end() {
        Bukkit.getScheduler().cancelTask(taskID);
        OfflinePlayer owner = Bukkit.getOfflinePlayer(this.owner);
        if (winning == null) {
            Messages.getMessager().messageListeningAll(this, "auction-end-no-bidders", true, true);
            // Return items to owner
            if (!owner.isOnline()) {
                System.out.print("Saving items of " + owner.getName());
                plugin.save(this.owner, item);
            } else {
                // return items to owner
                Player player = (Player) owner;
                plugin.giveItem(player, item, "nobidder-return");
            }
            Auction.getAuctionManager().remove(this);
            return true;
        }
        OfflinePlayer winner = Bukkit.getOfflinePlayer(winning);
        if (winner.isOnline()) {
            Player winner1 = (Player) winner;
            plugin.giveItem(winner1, item);
            Messages.getMessager().sendText(winner1, this, "auction-winner", true);
            //plugin.getMessageFormatted("auction-winner").send(winner1);
        } else {
            // Save the items
            YamlConfiguration logoff = plugin.getLogOff();
            if (logoff.getString(winner.getUniqueId().toString()) != null) {

            } else {
                plugin.save(winning, item);
            }
        }

        double winnings = topBid;
        if (taxable) {
            winnings -= getCurrentTax();
        }
        Auction.getEconomy().depositPlayer(owner.getName(), winnings);
        Messages.getMessager().messageListeningAll(this, "auction-end-broadcast", true, true);
        if (owner.isOnline()) {
            Player player = (Player) owner;
            Messages.getMessager().sendText(player, this, "auction-ended", true);
            if (taxable) {
                Messages.getMessager().sendText(player, this, "auction-end-tax", true);
            }
        }
        Auction.getAuctionManager().remove(this);
        return true;
    }

    private boolean searchInventory(Player player) {
        int count = 0;
        for (ItemStack is : player.getInventory()) {
            if (is != null) { 
                if (is.isSimilar(item)) {
                    if (is.getAmount() >= numItems) {
                        return true;
                    } else {
                        count += is.getAmount();
                    }
                }
            }
        }
        if (count >= numItems) { 
            return true;
        }
        return false;
    }

    private String getFormattedTime() {		
        String formatted = "";
        int days = (int) Math.floor(timeLeft / 86400); // get days
        int hourSeconds = timeLeft % 86400; 
        int hours = (int) Math.floor(hourSeconds / 3600); // get hours
        int minuteSeconds = hourSeconds % 3600;
        int minutes = (int) Math.floor(minuteSeconds / 60); // get minutes
        int remainingSeconds = minuteSeconds % 60;
        int seconds = (int) Math.ceil(remainingSeconds); // get seconds

        if (days > 0) formatted += String.format("%d d, ", days);
        if (hours > 0) formatted += String.format("%d hr, ", hours);
        if (minutes > 0) formatted += String.format("%d min, ", minutes);
        if (seconds > 0) formatted += String.format("%d sec", seconds);

        return formatted;
    }

    @SuppressWarnings("serial")
    public class InsufficientItemsException extends Exception {

    }

    @SuppressWarnings("serial")
    public class EmptyHandException extends Exception {

    }

    @SuppressWarnings("serial")
    public class UnsupportedItemException extends Exception {

    }
}


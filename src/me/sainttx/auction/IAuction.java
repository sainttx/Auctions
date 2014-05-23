package me.sainttx.auction;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class IAuction {
    private Auction plugin;

    private boolean taxable = false;

    private String worldName;
    private UUID owner; // The person who started the auction
    private UUID winning; // Current top bidder

    private ItemStack item; // The item being auctioned
    private int numItems; // Amount in the ItemStack
    private double increment; // Minimum increment to bid
    private double autoWin; // The autowin (if set)
    private double currentBid; // Current top bidder

    private int auctionTimer;
    private int timeLeft;

    private final int[] times = {45, 30, 10, 3, 2, 1}; // Countdown time to announce

    public IAuction(Auction plugin, Player player, int numItems, int startingAmount, int autoWin)
            throws InsufficientItemsException, EmptyHandException, UnsupportedItemException {
        this.plugin = plugin;
        this.owner = player.getUniqueId();
        this.numItems = numItems;
        this.item = player.getItemInHand().clone();
        this.item.setAmount(numItems);
        this.currentBid = startingAmount;
        this.increment = plugin.getIncrement();
        this.worldName = player.getWorld().getName();
        this.timeLeft = plugin.getAuctionStartTime();
        this.autoWin = autoWin;

        if (autoWin < currentBid + increment && autoWin != -1) {
            this.autoWin = currentBid + increment;
        }

        validAuction(player);
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
        return currentBid;
    }

    public void setTopBid(int topBid) {
        this.currentBid = topBid;
    }

    public int getNumItems() {
        return numItems;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getIncrement() {
        return increment;
    }

    public double getAutoWin() {
        return autoWin;
    }

    public double getCurrentTax() {
        int tax = plugin.getAuctionTaxPercentage();
        return (currentBid * tax) / 100;
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

    public void start() { // TODO: Check this 
        final Messages messager = Messages.getMessager();
        
        auctionTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new AuctionTimer(this), 0L, 20L);
        
        if (plugin.isPerWorldAuctions()) {
            if (plugin.getTellOtherWorldsStart()) {
                messager.messageListeningAllOther(this, "auction-in-other-world", true);
            }
            messager.messageListeningAll(this, "auction-start", true, true);
            messager.messageListeningAll(this, "auction-start-price", true, false);
        } else {
            messager.messageListeningAll(this, "auction-start", true, true); 
            messager.messageListeningAll(this, "auction-start-price", true, true);
        }
        
        if (autoWin != -1) {
            messager.messageListeningAll(this, "auction-start-autowin", true, true);
        }
    }
        
//        
//        if (!plugin.getTellOtherWorldsStart()) {
//            messager.messageListeningAll(this, "auction-start", true, true); 
//            messager.messageListeningAll(this, "auction-start-price", true, true);
//        }
//        
//        if (plugin.getTellOtherWorldsStart()) {
//            if (plugin.isPerWorldAuctions()) {
//                messager.messageListeningAllOther(this, "auction-in-other-world", true);
//            }
//            messager.messageListeningAll(this, "auction-start", true, true);
//            messager.messageListeningAll(this, "auction-start-price", true, false);
//        } else {
//            messager.messageListeningAll(this, "auction-start", true, true); 
//            messager.messageListeningAll(this, "auction-start-price", true, true);
//        }

    public void end() {
        Bukkit.getScheduler().cancelTask(auctionTimer);
        
        Player owner = Bukkit.getPlayer(this.owner);
        
        if (winning == null) { // Nobody placed a bid
            
            return;
        }
        
        
        if (owner != null) {
            
            
        } else {
            
        }
        
        
        
        //OfflinePlayer owner = Bukkit.getOfflinePlayer(this.owner);
        if (winning == null) {
            Messages.getMessager().messageListeningAll(this, "auction-end-no-bidders", true, true);
            // Return items to owner
            if (!owner.isOnline()) {
                System.out.print("[Auction] Saving items of offline player " + owner.getName());
                plugin.save(this.owner, item);
            } else {
                // return items to owner
                Player player = (Player) owner;
                plugin.giveItem(player, item, "nobidder-return");
            }
            Auction.getAuctionManager().removeAuctionFromMemory(this);
            return;
        }
        OfflinePlayer winner = Bukkit.getOfflinePlayer(winning);
        if (winner.isOnline()) {
            Player winner1 = (Player) winner;
            plugin.giveItem(winner1, item);
            Messages.getMessager().sendText(winner1, this, "auction-winner", true);
        } else {
            // Save the items
            System.out.print("[Auction] Saving items of offline player " + owner.getName());
            plugin.save(winning, item);
        }

        double winnings = currentBid;
        if (taxable) {
            winnings -= getCurrentTax();
        }
        Auction.economy.depositPlayer(owner.getName(), winnings);
        Messages.getMessager().messageListeningAll(this, "auction-end-broadcast", true, true);
        if (owner.isOnline()) {
            Player player = (Player) owner;
            Messages.getMessager().sendText(player, this, "auction-ended", true);
            if (taxable) {
                Messages.getMessager().sendText(player, this, "auction-end-tax", true);
            }
        }
        Auction.getAuctionManager().removeAuctionFromMemory(this);
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

    public class AuctionTimer extends BukkitRunnable {

        private IAuction auction;
        
        public AuctionTimer(IAuction auction) {
            this.auction = auction;
        }
        
        @Override
        public void run() {
            if (timeLeft <= 0) {
                end();
            } else {
                --timeLeft;
                for (int i : times) {
                    if (i == timeLeft) {
                        Messages.getMessager().messageListeningAll(auction, "auction-timer", true, true);
                        //plugin.messageListening(plugin.getMessageFormatted("auction-timer"));
                        break;
                    }
                }
            }
        }
    }


    /* Verifies that this auction has valid settings */
    private void validAuction(Player player) throws EmptyHandException, UnsupportedItemException, InsufficientItemsException {
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

}


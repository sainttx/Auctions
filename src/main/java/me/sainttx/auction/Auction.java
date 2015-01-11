package me.sainttx.auction;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Auction {
    private AuctionPlugin plugin;
    private AuctionManager manager;
    private Messages messager;

    private boolean taxable = false;

    private @Getter UUID owner; // The person who started the auction
    private @Getter @Setter UUID winning; // Current top bidder

    private ItemStack item; // The item being auctioned
    private @Getter int numItems; // Amount in the ItemStack
    private @Getter double autoWin; // The autowin (if set)
    private @Getter @Setter double topBid; // Current top bidder

    private int auctionTimer;
    private @Getter int timeLeft;

    private final int[] times = { 45, 30, 15, 10, 3, 2, 1 }; // Countdown time to announce

    /**
     * Instantiate an Auction
     * 
     * @param plugin The AuctionPlugin auction
     * @param player The player who begun the auction
     * @param numItems The number of items being auctioned
     * @param startingAmount The starting amount specified by the player
     * @param autoWin The amount that will automatically end the auction
     * 
     * @throws Exception If the player auctioned nothing, 
     *                   If the player auctioned a banned item,
     *                   If the player does not have enough items to auction
     */
    public Auction(AuctionPlugin plugin, Player player, int numItems, double startingAmount, double autoWin) throws Exception {
        this.plugin     = plugin;
        this.manager    = AuctionManager.getAuctionManager();
        this.messager   = Messages.getMessager();
        this.owner      = player.getUniqueId();
        this.numItems   = numItems;
        this.topBid     = startingAmount;
        this.timeLeft   = plugin.getDefaultAuctionTime();
        this.autoWin    = autoWin;
        this.item       = player.getItemInHand().clone();
        this.item.setAmount(numItems);
        if (autoWin < topBid + plugin.getMinBidIncrement() && autoWin != -1) {
            this.autoWin = topBid + plugin.getMinBidIncrement();
        }

        validateAuction(player);
    }

    /**
     * Returns a cloned copy of the item being auctioned
     * 
     * @return ItemStack the item being auctioned
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /**
     * Returns the current taxation on the auction
     * 
     * @return Double the tax on the auction
     */
    public double getCurrentTax() {
        int tax = plugin.getTaxPercentage();
        return (topBid * tax) / 100;
    }

    /**
     * Returns whether or not the auction has bids placed on it
     * 
     * @return True if somebody has bid on the auction, false otherwise
     */
    public boolean hasBids() {
        return winning != null;
    }

    /**
     * Gets the time remaining as a String
     * 
     * @return String a formatted representation of time left
     */
    public String getTime() {
        return AuctionUtil.getFormattedTime(timeLeft);
    }

    /**
     * Sets whether or not the auction can be taxed
     * 
     * @param taxable If the auction can be taxed
     */
    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }

    /**
     * Begins the auction
     */
    public void start() {
        auctionTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new AuctionTimer(this), 0L, 20L);
        messager.messageListeningAll(this, "auction-start", true); 
        messager.messageListeningAll(this, "auction-start-price", true);

        if (autoWin != -1) {
            messager.messageListeningAll(this, "auction-start-autowin", true);
        }
    }

    @SuppressWarnings("static-access")
    /**
     * Ends the auction
     * 
     * @param broadcast Whether or not to broadcast messages that this auction has ended
     */
    public void end(boolean broadcast) {
        Bukkit.getScheduler().cancelTask(auctionTimer);

        // Delay before a new auction can be made... Prevents auction scamming
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(AuctionPlugin.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    manager.setCanAuction(true);
                    
                    // Start the next auction in the queue
                    if (AuctionManager.getCurrentAuction() == null) {
                        manager.startNextAuction();
                    }
                }
            }, 30L);
        }

        Player owner = Bukkit.getPlayer(this.owner);

        // Check if somebody won the auction
        if (winning != null) {
            Player winner = Bukkit.getPlayer(winning);

            // Check if the winner is online
            if (winner != null) {
                AuctionUtil.giveItem(winner, item);
                messager.sendText(winner, this, "auction-winner", true);
            } else {
                System.out.print("[Auction] Saving items of offline player " + this.winning);
                plugin.save(winning, item);
            }

            double winnings = topBid - (taxable ? getCurrentTax() : 0);
            plugin.economy.depositPlayer(owner, winnings);

            if (broadcast) {
                messager.messageListeningAll(this, "auction-end-broadcast", true);
            }
            
            // Check if the owner of the auction is online
            if (owner != null) { 
                messager.sendText(owner, this, "auction-ended", true);
                if (taxable) {
                    messager.sendText(owner, this, "auction-end-tax", true);
                }
            }
        }

        // There was no winner
        else {
            if (broadcast) {
                messager.messageListeningAll(this, "auction-end-no-bidders", true);
            } 
            
            // Check if we can give the items back to the owner (if they're online)
            if (owner != null) {
                AuctionUtil.giveItem((Player) owner, item, "nobidder-return");
            } else {
                System.out.print("[Auction] Saving items of offline player " + this.owner);
                plugin.save(this.owner, item);
            }
        } 

        // Set the current auction to null
        manager.killAuction(); 
    }

    public class AuctionTimer extends BukkitRunnable {

        private Auction auction;

        public AuctionTimer(Auction auction) {
            this.auction = auction;
        }

        @Override
        public void run() {
            if (timeLeft <= 0) {
                end(true);
            } else {
                --timeLeft;
                for (int i : times) {
                    if (i == timeLeft) {
                        messager.messageListeningAll(auction, "auction-timer", true);
                        break;
                    }
                }
            }
        }
    }

    /* Verifies that this auction has valid settings */
    private void validateAuction(Player player) throws Exception {
        // Check if they actually auctioned an item
        if (item.getType() == Material.AIR) {
            throw new Exception("fail-start-handempty");
        }
        
        // Check if the item is allowed
        if (item.getType() == Material.FIREWORK || item.getType() == Material.FIREWORK_CHARGE || AuctionManager.getBannedMaterials().contains(item.getType())) {
            throw new Exception("unsupported-item");
        }
        
        // Check if they have enough of the item
        if (AuctionUtil.searchInventory(player.getInventory(), item, numItems)) {
            player.getInventory().removeItem(item);
        } else {
            throw new Exception("fail-start-not-enough-items");
        }
    }
}


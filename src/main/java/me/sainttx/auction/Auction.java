package me.sainttx.auction;

import me.sainttx.auction.util.AuctionUtil;
import me.sainttx.auction.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {
    private AuctionPlugin plugin;

    private String ownerName; // The name of the person that started the auction
    private UUID owner; // The person who started the auction
    private UUID winning; // Current top bidder

    private ItemStack item; // The item being auctioned

    /*
     * Auction information
     */
    private boolean taxable = false; // Whether or not taxes should be applied on this auction
    private double autoWin; // The autowin (if set)
    private double topBid; // Current top bidder
    private int numItems; // Amount in the ItemStack
    private int auctionTimer; // The auction timer task id
    private int timeLeft;

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
        this.ownerName  = player.getName();
        this.owner      = player.getUniqueId();
        this.numItems   = numItems;
        this.topBid     = startingAmount;
        this.timeLeft   = plugin.getConfig().getInt("auction-time", 30);
        this.autoWin    = autoWin;
        this.item       = player.getItemInHand().clone();
        this.item.setAmount(numItems);
        if (autoWin < topBid + plugin.getConfig().getDouble("minimum-bid-increment", 1D) && autoWin != -1) {
            this.autoWin = topBid + plugin.getConfig().getDouble("minimum-bid-increment", 1D);
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
        int tax = plugin.getConfig().getInt("auction-tax-percentage", 0);
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
        TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start")), Bukkit.getOnlinePlayers().toArray(new Player[0]));
        TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start-price")), Bukkit.getOnlinePlayers().toArray(new Player[0]));

        if (autoWin != -1) {
            TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start-autowin")), Bukkit.getOnlinePlayers().toArray(new Player[0]));
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
                    AuctionManager.getAuctionManager().setCanAuction(true);

                    // Start the next auction in the queue
                    if (AuctionManager.getCurrentAuction() == null) {
                        AuctionManager.getAuctionManager().startNextAuction();
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
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-winner")), winner);
            } else {
                Bukkit.getLogger().info("[Auction] Saving items of offline player " + this.winning);
                plugin.saveOfflinePlayer(winning, item);
            }

            double winnings = topBid - (taxable ? getCurrentTax() : 0);
            AuctionPlugin.getEconomy().depositPlayer(owner, winnings);

            if (broadcast) {
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-end-broadcast")), Bukkit.getOnlinePlayers().toArray(new Player[0]));
            }

            // Check if the owner of the auction is online
            if (owner != null) {
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-ended")), owner);
                if (taxable) {
                    TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-end-tax")), owner);
                }
            }
        }

        // There was no winner
        else {
            if (broadcast) {
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-end-no-bidders")), Bukkit.getOnlinePlayers().toArray(new Player[0]));
            }

            // Check if we can give the items back to the owner (if they're online)
            if (owner != null) {
                AuctionUtil.giveItem(owner, item, "nobidder-return");
            } else {
                Bukkit.getLogger().info("[Auction] Saving items of offline player " + this.owner);
                plugin.saveOfflinePlayer(this.owner, item);
            }
        }

        // Set the current auction to null
        AuctionManager.getAuctionManager().killAuction();
    }

    /**
     * An Auction timer that counts down an Auction until it's over
     */
    protected class AuctionTimer implements Runnable {

        /*
         * The Auction to count down
         */
        private Auction auction;

        /**
         * Create an Auction timer
         */
        public AuctionTimer(Auction auction) {
            this.auction = auction;
        }

        @Override
        /*
         * Decrement the current time left until the auction ends
         */
        public void run() {
            if (timeLeft <= 0) {
                end(true);
            } else {
                --timeLeft;
                switch(timeLeft) {
                    case 120:
                    case 90:
                    case 60:
                    case 45:
                    case 30:
                    case 15:
                    case 10:
                    case 3:
                    case 2:
                    case 1:
                        TextUtil.sendMessage(TextUtil.replace(auction, TextUtil.getConfigMessage("auction-timer")), Bukkit.getOnlinePlayers().toArray(new Player[0]));
                }
            }
        }
    }

    /*
     * Verifies that this auction has valid settings
     */
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

    public String getOwnerName() {
        return ownerName;
    }

    public double getTopBid() {
        return topBid;
    }

    public UUID getWinning() {
        return winning;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getNumItems() {
        return numItems;
    }

    public double getAutoWin() {
        return autoWin;
    }

    public void setWinning(UUID winning) {
        this.winning = winning;
    }

    public void setTopBid(double topBid) {
        this.topBid = topBid;
    }
}


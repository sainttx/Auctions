package me.sainttx.auction;

import me.sainttx.auction.util.AuctionUtil;
import me.sainttx.auction.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Auction {

    /*
     * The auction plugin
     */
    private AuctionPlugin plugin;

    /*
     * Used for the AuctionTimer
     */
    protected static List<Integer> broadcastTimes = new ArrayList<Integer>();

    /*
     * Auction owner information
     */
    private String ownerName;   // The name of the person that started the auction
    private UUID owner;         // The UUID of the person who started the auction

    /*
     * Bidder information
     */
    private String winningName; // Current top bidders name
    private UUID winning;       // Current top bidders UUID
    private double topBid;      // Current top bid amount

    /*
     * Auction information
     */
    private ItemStack item;          // The item being auctioned
    private boolean taxable = false; // Whether or not taxes should be applied on this auction
    private double autoWin;          // The auto-win amount (if set)
    private int bidIncrement;        // The bid increment
    private int numItems;            // Amount in the ItemStack
    private int auctionTimer;        // The auction timer task id
    private int timeLeft;            // The amount of time left in this auction
    private int antiSniped;          // The amount of times anti-snipe has gone off for this auction

    /**
     * Instantiate an Auction
     *
     * @param plugin         The AuctionPlugin auction
     * @param player         The player who begun the auction
     * @param numItems       The number of items being auctioned
     * @param startingAmount The starting amount specified by the player
     * @param autoWin        The amount that will automatically end the auction
     * @throws Exception If the player auctioned nothing,
     *                   If the player auctioned a banned item,
     *                   If the player does not have enough items to auction
     */
    public Auction(AuctionPlugin plugin, Player player, int numItems, double startingAmount, int bidIncrement, double autoWin) throws Exception {
        this.plugin = plugin;
        this.ownerName = player.getName();
        this.owner = player.getUniqueId();
        this.numItems = numItems;
        this.topBid = startingAmount;
        this.timeLeft = plugin.getConfig().getInt("default-auction-start-time", 30);
        this.bidIncrement = bidIncrement;
        this.autoWin = autoWin;
        this.item = player.getItemInHand().clone();
        this.item.setAmount(numItems);
        if (autoWin < topBid + plugin.getConfig().getDouble("default-bid-increment", 10D) && autoWin != -1) {
            this.autoWin = topBid + plugin.getConfig().getDouble("default-bid-increment", 10D);
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
     * Returns the time, in seconds, left in the auction
     *
     * @return The time, in seconds, until the auction ends
     */
    public int getTimeLeft() {
        return this.timeLeft;
    }

    /**
     * Adds seconds to the auctions timer
     *
     * @param seconds The amount of seconds to add
     */
    public void addSeconds(int seconds) {
        this.timeLeft += seconds;
    }

    /**
     * Returns the amount of times anti-snipe has activated on the auction
     *
     * @return the
     */
    public int getAntiSniped() {
        return this.antiSniped;
    }

    /**
     * Increments the amount of times this auction has been sniped
     */
    public void incrementAntiSniped() {
        this.antiSniped++;
    }

    /**
     * Begins the auction
     */
    public void start() {
        auctionTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new AuctionTimer(this), 0L, 20L);
        TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
        TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start-price")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
        TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start-increment")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));

        if (autoWin != -1) {
            TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-start-autowin")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
        }
    }

    /**
     * Cancels this auction
     */
    public void cancel() {
        // Schedule stuff
        Bukkit.getScheduler().cancelTask(auctionTimer);
        if (plugin.isEnabled()) {
            runNextAuctionTimer();
        }

        Player owner = Bukkit.getPlayer(this.owner);

        // Return the item to the owner
        if (owner != null) {
            AuctionUtil.giveItem(owner, item);
        } else {
            Bukkit.getLogger().info("[Auction] Saving items of offline player " + this.owner);
            plugin.saveOfflinePlayer(this.owner, item);
        }

        // Give back the top bidders money
        if (winning != null) {
            OfflinePlayer topBidder = Bukkit.getPlayer(winning);
            AuctionPlugin.getEconomy().depositPlayer(topBidder, topBid);
        }

        // Broadcast
        TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-cancelled")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));

        // Set the current auction to null
        AuctionManager.getAuctionManager().killAuction();
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
            runNextAuctionTimer();
        }

        Player owner = Bukkit.getPlayer(this.owner);

        // Check if somebody won the auction
        if (winning != null) {
            Player winner = Bukkit.getPlayer(winning);

            // Check if the winner is online
            if (winner != null) {
                AuctionUtil.giveItem(winner, item);
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-winner")), true, winner);
            } else {
                Bukkit.getLogger().info("[Auction] Saving items of offline player " + this.winning);
                plugin.saveOfflinePlayer(winning, item);
            }

            double winnings = topBid - (taxable ? getCurrentTax() : 0);
            AuctionPlugin.getEconomy().depositPlayer(owner == null ? Bukkit.getOfflinePlayer(this.owner) : owner, winnings);

            if (broadcast) {
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-end-broadcast")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
            }

            // Check if the owner of the auction is online
            if (owner != null) {
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-ended")), true, owner);
                if (taxable) {
                    TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-end-tax")), true, owner);
                }
            }
        } else { // There was no winner
            if (broadcast) {
                TextUtil.sendMessage(TextUtil.replace(this, TextUtil.getConfigMessage("auction-end-no-bidders")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
            }

            // Check if we can give the items back to the owner (if they're online)
            if (owner != null) {
                AuctionUtil.giveItem(owner, item, "no-bidder-return");
            } else {
                Bukkit.getLogger().info("[Auction] Saving items of offline player " + this.owner);
                plugin.saveOfflinePlayer(this.owner, item);
            }
        }

        // Set the current auction to null
        AuctionManager.getAuctionManager().killAuction();
    }

    /*
     * Runs the timer that schedules a new auction after an auction is ended
     */
    private void runNextAuctionTimer() {
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
            }, plugin.getConfig().getLong("delay-between-auctions-seconds", 5L) * 20L);
        }
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
                if (broadcastTimes.contains(--timeLeft)) {
                    TextUtil.sendMessage(TextUtil.replace(auction, TextUtil.getConfigMessage("auction-timer")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
                }
            }
        }
    }

    /*
     * Verifies that this auction has valid settings
     */
    private void validateAuction(Player player) throws Exception {
        if (item == null || item.getType() == Material.AIR) {
            // They auctioned off nothing
            throw new Exception("fail-start-hand-empty");
        } else if (item.getType() == Material.FIREWORK || item.getType() == Material.FIREWORK_CHARGE || AuctionManager.getBannedMaterials().contains(item.getType())) {
            // The item isn't allowed
            throw new Exception("unsupported-item");
        } else if (item.getType().getMaxDurability() > 0 && item.getDurability() > 0 && !plugin.getConfig().getBoolean("allow-damaged-items", true)) {
            // Users can't auction damaged items
            throw new Exception("fail-start-damaged-item");
        } else if (!AuctionUtil.searchInventory(player.getInventory(), item, numItems)) {
            // They don't have enough of that item in their inventory
            throw new Exception("fail-start-not-enough-items");
        } else if (!plugin.getConfig().getBoolean("allow-auctioning-named-items", true) && item.getItemMeta().hasDisplayName()) {
            // The player can't auction named items
            throw new Exception("fail-start-named-item");
        } else if (hasBannedLore()) {
            // The players item contains a piece of denied lore
            throw new Exception("fail-start-banned-lore");
        } else {
            player.getInventory().removeItem(item);
        }
    }

    /*
     * Check if an item has a denied String of lore
     */
    private boolean hasBannedLore() {
        List<String> bannedLore = plugin.getConfig().getStringList("banned-lore");

        if (bannedLore != null && !bannedLore.isEmpty()) {
            if (item.getItemMeta().hasLore()) {
                List<String> lore = item.getItemMeta().getLore();

                for (String loreItem : lore) {
                    for (String banned : bannedLore) {
                        if (loreItem.contains(banned)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    /**
     * Returns the owner's UUID
     *
     * @return The player that started this auction's UUID
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Returns the Auction owners name
     *
     * @return the name of the Player that started this Auction
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Returns the current top bid
     *
     * @return The value of the highest bid
     */
    public double getTopBid() {
        return topBid;
    }

    /**
     * Returns the name of the current top bidder
     *
     * @return The name of the current top bidder
     */
    public String getWinningName() {
        return this.winningName;
    }

    /**
     * Returns the ID of the player who currently has the highest bid
     *
     * @return the UUID of the current highest bidder
     */
    public UUID getWinning() {
        return winning;
    }

    /**
     * The new top bidder
     *
     * @param winning The new top bidder
     */
    public void setWinning(Player winning) {
        this.winning = winning.getUniqueId();
        this.winningName = winning.getName();
    }

    /**
     * Gets the number of items in the auction
     *
     * @return The ItemStacks amount value
     */
    public int getNumItems() {
        return numItems;
    }

    /**
     * Gets the auto win amount
     *
     * @return The auto win amount
     */
    public double getAutoWin() {
        return autoWin;
    }

    /**
     * Sets the new top bid for this Auction
     *
     * @param topBid the new top bid value
     */
    public void setTopBid(double topBid) {
        this.topBid = topBid;
    }

    /**
     * Returns this auctions bid increment
     *
     * @return The bid increment for this auction
     */
    public double getBidIncrement() {
        return bidIncrement;
    }
}


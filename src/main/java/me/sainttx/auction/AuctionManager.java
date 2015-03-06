package me.sainttx.auction;

import me.sainttx.auction.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuctionManager {

    /*
     * Auction plugin and manager instances
     */
    private static AuctionPlugin plugin;
    private static AuctionManager manager;

    /*
     * The current ongoing auction
     */
    private static Auction currentAuction;

    /*
     * The auction queue
     */
    private Queue<Auction> auctionQueue = new ConcurrentLinkedQueue<Auction>();

    /*
     * Banned materials in an auction
     */
    private static ArrayList<Material> banned = new ArrayList<Material>();

    /*
     * Information on whether an auction can be started
     */
    private boolean disabled = false;
    private boolean canAuction = true;

    /**
     * Creates the Auction Manager
     */
    private AuctionManager() {
        plugin = AuctionPlugin.getPlugin();
        storeBannedItems();
    }

    /**
     * Returns the AuctionManager instance, creates a new manager if it has
     * never been instantiated
     *
     * @return AuctionManager The AuctionManager instance
     */
    public static AuctionManager getAuctionManager() {
        return manager == null ? manager = new AuctionManager() : manager;
    }


    @SuppressWarnings("unchecked")
    /**
     * Returns a deep copy of banned materials
     *
     * @return ArrayList<Material> Materials not allowed in auctions
     */
    public static ArrayList<Material> getBannedMaterials() {
        return (ArrayList<Material>) banned.clone();
    }

    /**
     * Returns the Auction queue
     *
     * @return All of the auctions that are currently queued
     */
    public Queue<Auction> getAuctionQueue() {
        return this.auctionQueue;
    }

    /**
     * Returns the position a player is in the queue
     *
     * @param player The player to check
     * @return The position in the queue that the player is in, -1 if not in the queue
     */
    public int getQueuePosition(Player player) {
        Auction[] queueArray = auctionQueue.toArray(new Auction[0]);

        for (int i = 0 ; i < queueArray.length ; i++) {
            Auction auc = queueArray[i];
            if (auc != null && auc.getOwner().equals(player.getUniqueId())) {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * Returns whether or not a player has an auction queued
     *
     * @param player A player who may have an auction queued
     * @return True if the player has an auction queued, false otherwise
     */
    public static boolean hasAuctionQueued(Player player) {
        for (Auction queued : manager.auctionQueue) {
            if (queued.getOwner().equals(player.getUniqueId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether or not a player is hosting an active auction
     *
     * @param player A player who may be participating in an auction
     * @return True if the player is the owner of the current auction
     */
    public static boolean isAuctioningItem(Player player) {
        return currentAuction != null && currentAuction.getOwner().equals(player.getUniqueId());
    }

    /**
     * Gives information about the auction to a Player
     *
     * @param player The player to receive auction information
     */
    public void sendAuctionInfo(Player player) {
        if (currentAuction != null) {
            TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("auction-info-message")), true, player);
            TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("auction-start-increment")), true, player);

            int queuePosition = getQueuePosition(player);
            if (queuePosition > 0) {
                TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("auction-queue-position").replaceAll("%q", Integer.toString(queuePosition))), true, player);
            }
        } else {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-info-no-auction"), true, player);
        }
    }

    /**
     * Performs pre-checks for creating an auction started by a player
     *
     * @param player The player who started the auction
     * @param args   Arguments relative to the auction provided by the player
     */
        public void prepareAuction(Player player, String[] args) {
        double minStartingPrice = plugin.getConfig().getDouble("minimum-auction-start-price", 0);
        double maxStartingPrice = plugin.getConfig().getDouble("maximum-auction-start-price", Integer.MAX_VALUE);

        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            // Auctioning is disabled at the moment
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-auction-disabled"), true, player);
        } else if (args.length < 3) {
            // Arguments don't match the required length
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-syntax"), true, player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            // The player is ignoring auctions
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-ignoring"), true, player);
        } else {
            int numItems;
            double startingPrice;
            int bidIncrement = plugin.getConfig().getInt("default-bid-increment", 50);
            double autoWin = -1;
            double fee = plugin.getConfig().getDouble("auction-start-fee", 0);

            try {
                numItems = Integer.parseInt(args[1]);
                startingPrice = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-number-format"), true, player);
                return;
            }

            if (numItems < 0) {
                // Item amount provided was negative
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-negative-number"), true, player);
            } else if (startingPrice < minStartingPrice) {
                // Invalid price (under the minimum)
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-min"), true, player);
            } else if (startingPrice > maxStartingPrice) {
                // Invalid price (over the maximum)
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-max"), true, player);
            } else if (fee > AuctionPlugin.getEconomy().getBalance(player)) {
                // Player doesn't have enough money
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-no-funds"), true, player);
            } else if (auctionQueue.size() > plugin.getConfig().getInt("queue-limit", 3)) {
                // The Auction queue is full
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-queue-full"), true, player);
            } else {
                try {
                    if (args.length >= 4) {
                        bidIncrement = Integer.parseInt(args[3]);

                        if (plugin.getConfig().getInt("minimum-bid-increment") > bidIncrement) {
                            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-bid-increment"), true, player);
                            return;
                        } else if (plugin.getConfig().getInt("maximum-bid-increment") < bidIncrement) {
                            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-bid-increment"), true, player);
                            return;
                        }
                    }
                    if (args.length == 5) {
                        double autowinAmount = Integer.parseInt(args[4]); // Autowin
                        autoWin = plugin.getConfig().getBoolean("allow-auction-auto-winning", false) ? autowinAmount : -1;

                        // Check if the player is allowed to specify an autowin
                        if (!plugin.getConfig().getBoolean("allow-auction-auto-winning", false)) {
                            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-no-autowin"), true, player);
                            return;
                        }
                    }
                } catch (NumberFormatException exception) {
                    TextUtil.sendMessage(TextUtil.getConfigMessage("fail-number-format"), true, player);
                    return;
                }

                // Decide whether to immediately start the auction or place it in the queue
                if (currentAuction != null && currentAuction.getOwner().equals(player.getUniqueId())) {
                    TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-already-auctioning"), true, player);
                } else if (hasAuctionQueued(player)) { // The player already has an auction queued
                    TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-already-queued"), true, player);
                } else {
                    Auction auction = createAuction(plugin, player, numItems, startingPrice, bidIncrement, autoWin);

                    if (auction != null) {
                        if (currentAuction == null && this.canAuction) {
                            startAuction(auction);
                        } else {
                            auctionQueue.add(auction);
                            TextUtil.sendMessage(TextUtil.getConfigMessage("auction-queued"), true, player);
                        }
                    }
                }
            }
        }
    }

    /**
     * Starts an auction and withdraws the starting fee
     *
     * @param auction The auction to begin
     */
    public void startAuction(Auction auction) {
        if (auction == null) {
            return;
        }

        AuctionPlugin.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(auction.getOwner()), plugin.getConfig().getDouble("auction-start-fee", 0));
        currentAuction = auction;
        auction.start();
        setCanAuction(false);
    }

    /**
     * Starts the next auction in the queue
     */
    public void startNextAuction() {
        Auction next = auctionQueue.poll();

        if (next != null) {
            startAuction(next);
        }
    }

    public void cancelCurrentAuction(Player player) {
        if (currentAuction == null) {
            // No auction
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-cancel-no-auction"), true, player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            // Ignoring
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-ignoring"), true, player);
        } else if (!plugin.getConfig().getBoolean("allow-auction-cancel-command", true) && !player.hasPermission("auction.cancel.bypass")) {
            // Can't cancel
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-cancel-disabled"), true, player);
        } else if (currentAuction.getTimeLeft() < plugin.getConfig().getInt("must-cancel-auction-before", 15) && !player.hasPermission("auction.cancel.bypass")) {
            // Can't cancel
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-cancel-time"), true, player);
        } else if (!currentAuction.getOwner().equals(player.getUniqueId()) && !player.hasPermission("auction.cancel.bypass")) {
            // Can't cancel other peoples auction
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-cancel-not-yours"), true, player);
        } else {
            currentAuction.cancel();
        }
    }

    /**
     * Creates an auction and verifies it was properly specified
     *
     * @param plugin        The Auction plugin
     * @param player        The player starting the auction
     * @param numItems      The number of items the player is auctioning
     * @param startingPrice The starting price of the auction
     * @param autoWin       The amount required to bid to automatically win
     * @return The auction result, null if something went wrong
     */
    public Auction createAuction(AuctionPlugin plugin, Player player, int numItems, double startingPrice, int bidIncrement, double autoWin) {
        Auction auction = null;
        try {
            auction = new Auction(AuctionPlugin.getPlugin(), player, numItems, startingPrice, bidIncrement, autoWin);
        } catch (NumberFormatException ex1) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-number-format"), true, player);
        } catch (Exception ex2) {
            TextUtil.sendMessage(TextUtil.getConfigMessage(ex2.getMessage()), true, player);
        }

        if (auction != null && !player.hasPermission("auction.tax.exempt")) {
            auction.setTaxable(true);
        }

        return auction;
    }

    /**
     * Formats String input provided by a player before proceeding to pre-bid
     * checking
     *
     * @param player The player bidding
     * @param amount The amount bid by the player
     */
    public void prepareBid(Player player, String amount) {
        try {
            double bid = Double.parseDouble(amount);
            prepareBid(player, bid);
        } catch (NumberFormatException ex) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-number"), true, player);
        }
    }

    @SuppressWarnings("static-access")
    /**
     * Prepares a bid by a player and verifies they have met requirements before
     * bidding
     *
     * @param player The player bidding
     * @param amount The amount bid by the player
     */
    public void prepareBid(Player player, double amount) {
        if (currentAuction == null) {
            // There is no auction to bid on
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-no-auction"), true, player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            // Player is ignoring Auctions
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-ignoring"), true, player);
        } else if (currentAuction.getOwner().equals(player.getUniqueId())) {
            // Players aren't allowed to bid on their own auction
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-your-auction"), true, player);
        } else if (amount < currentAuction.getTopBid() + currentAuction.getBidIncrement()) {
            // The player didn't bid enough
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-too-low"), true, player);
        } else if (AuctionPlugin.getEconomy().getBalance(player) < amount) {
            // The bid exceeds their balance
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-insufficient-balance"), true, player);
        } else if (currentAuction.getWinning() != null && currentAuction.getWinning().equals(player.getUniqueId())) {
            // The player already holds the highest bid
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-top-bidder"), true, player);
        } else {
            if (currentAuction.getWinning() != null) {
                Player oldWinner = Bukkit.getPlayer(currentAuction.getWinning());
                if (oldWinner != null) {
                    AuctionPlugin.getEconomy().depositPlayer(oldWinner, currentAuction.getTopBid());
                } else {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(currentAuction.getWinning());
                    AuctionPlugin.getEconomy().depositPlayer(offline, currentAuction.getTopBid());
                }
            }

            // Place the bid
            placeBid(player, amount);
        }
    }

    /**
     * Places a bid on the current auction by the player
     *
     * @param player The player placing the bid
     * @param amount The amount bid by the player
     */
    public void placeBid(Player player, double amount) {
        currentAuction.setTopBid(amount);
        currentAuction.setWinning(player);
        AuctionPlugin.getEconomy().withdrawPlayer(player, amount);

        // Check if the auction isn't won due to autowin
        if (amount >= currentAuction.getAutoWin() && currentAuction.getAutoWin() != -1) {
            TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("auction-ended-autowin")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
            currentAuction.end(true);
        } else {
            TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("bid-broadcast")), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));

            if (currentAuction.getTimeLeft() <= 3 && plugin.getConfig().getBoolean("enable-anti-snipe", true) && currentAuction.getAntiSniped() + 1 <= plugin.getConfig().getInt("anti-snipe-max-per-auction")) {
                int time = plugin.getConfig().getInt("anti-snipe-add-seconds", 5);
                if (time > 0) {
                    TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("anti-snipe-add").replace("%t", Integer.toString(time))), false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
                    currentAuction.addSeconds(time);
                    currentAuction.incrementAntiSniped();
                }
            }
        }
    }

    /**
     * Called when a player ends an auction
     *
     * @param player The player who ended the auction
     */
    public void end(Player player) {
        if (currentAuction == null) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-end-no-auction"), true, player);
        } else if (!plugin.getConfig().getBoolean("allow-auction-end-command", false) && !player.hasPermission("auction.end.bypass")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-end-disallowed"), true, player);
        } else if (!currentAuction.getOwner().equals(player.getUniqueId()) && !player.hasPermission("auction.end.bypass")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-end-not-your-auction"), true, player);
        } else {
            currentAuction.end(true);
            killAuction();
        }
    }

    /**
     * Nulls the auction
     */
    public void killAuction() {
        currentAuction = null;
    }

    /* 
     * Loads all banned items into memory 
     */
    private void storeBannedItems() {
        for (String string : plugin.getConfig().getStringList("banned-items")) {
            Material material = Material.getMaterial(string);
            if (material != null) {
                banned.add(material);
            }
        }
    }

    /**
     * Returns the current ongoing Auction
     *
     * @return The current Auction
     */
    public static Auction getCurrentAuction() {
        return currentAuction;
    }

    /**
     * Returns whether or not auctioning is disabled
     *
     * @return True if auctioning is disabled, false otherwise
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets the disabled status of the Auction plugin
     *
     * @param disabled The new Auction plugin status
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Sets whether players can auction or not
     *
     * @param canAuction The new value of auctioning availability
     */
    public void setCanAuction(boolean canAuction) {
        this.canAuction = canAuction;
    }
}
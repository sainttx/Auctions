package com.sainttx.auction;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.messages.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuctionManagerImpl implements AuctionManager {

    // Instance
    private static AuctionManagerImpl manager = new AuctionManagerImpl();

    // Auctions information
    private Auction currentAuction;
    private Queue<Auction> auctionQueue = new ConcurrentLinkedQueue<Auction>();
    private static ArrayList<Material> banned = new ArrayList<Material>();
    private boolean disabled;
    private boolean canAuction = true;

    private AuctionManagerImpl() {
        if (manager != null) {
            throw new IllegalStateException("cannot create new instances of the manager");
        }

        loadBannedMaterials();
    }

    /**
     * Singleton. Returns the auction manager instance.
     *
     * @return the only auction manager instance
     */
    public static AuctionManagerImpl getAuctionManager() {
        if (manager == null) { // Should never happen
            manager = new AuctionManagerImpl();
            Bukkit.getLogger().info("Created a new auction manager instance.");
        }

        return manager;
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

    @Override
    public int getQueuePosition(Player player) {
        int position = 0;

        for (Auction auction : getQueue()) {
            if (player.getUniqueId().equals(auction.getOwner())) {
                return position + 1;
            }

            position++;
        }

        return -1;
    }

    @Override
    public boolean hasAuctionInQueue(Player player) {
        for (Auction auction : getQueue()) {
            if (player.getUniqueId().equals(auction.getOwner())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasActiveAuction(Player player) {
        return getCurrentAuction() != null && player.getUniqueId().equals(getCurrentAuction().getOwner());
    }

    @Override
    public boolean isAuctioningDisabled() {
        return disabled;
    }

    @Override
    public void setAuctioningDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public Auction getCurrentAuction() {
        return currentAuction;
    }

    @Override
    public Queue<Auction> getQueue() {
        return auctionQueue;
    }

    @Override
    public void addAuctionToQueue(Auction auction) {
        getQueue().add(auction);
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        // TODO: Complete implementation
    }

    @Override
    public void startNextAuction() {
        Auction next = getQueue().poll();

        if (next != null) {
            next.start();
        }
    }

    /**
     * Gives information about the auction to a Player
     *
     * @param player The player to receive auction information
     */
    /* public void sendAuctionInfo(Player player) {
        if (currentAuction != null) {
            plugin.getMessageHandler().sendMessage(currentAuction, "auction-info-message", player);
            plugin.getMessageHandler().sendMessage(currentAuction, "auction-start-increment", player);

            int queuePosition = getQueuePosition(player);
            if (queuePosition > 0) {
                TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("auction-queue-position")
                        .replaceAll("%q", Integer.toString(queuePosition))), true, player);
            }
        } else {
            plugin.getMessageHandler().sendMessage("fail-info-no-auction", player);
        }
    } */

    /**
     * Performs pre-checks for creating an auction started by a player
     *
     * @param player The player who started the auction
     * @param args   Arguments relative to the auction provided by the player
     */
    /* public void prepareAuction(Player player, String[] args) {
        double minStartingPrice = plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0);
        double maxStartingPrice = plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999);

        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            // Auctioning is disabled at the moment
            plugin.getMessageHandler().sendMessage("fail-start-auction-disabled", player);
        } else if (args.length < 3) {
            // Arguments don't match the required length
            plugin.getMessageHandler().sendMessage("fail-start-syntax", player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            // The player is ignoring auctions
            plugin.getMessageHandler().sendMessage("fail-start-ignoring", player);
        } else {
            int numItems;
            double startingPrice;
            int bidIncrement = plugin.getConfig().getInt("auctionSettings.defaultBidIncrement", 50);
            double autoWin = -1;
            double fee = plugin.getConfig().getDouble("auctionSettings.startFee", 0);

            try {
                numItems = Integer.parseInt(args[1]);
                startingPrice = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                plugin.getMessageHandler().sendMessage("fail-number-format", player);
                return;
            }

            if (numItems < 0) {
                // Item amount provided was negative
                plugin.getMessageHandler().sendMessage("fail-start-negative-number", player);
            } else if (startingPrice < minStartingPrice) {
                // Invalid price (under the minimum)
                plugin.getMessageHandler().sendMessage("fail-start-min", player);
            } else if (startingPrice > maxStartingPrice) {
                // Invalid price (over the maximum)
                plugin.getMessageHandler().sendMessage("fail-start-max", player);
            } else if (fee > AuctionPlugin.getEconomy().getBalance(player)) {
                // Player doesn't have enough money
                plugin.getMessageHandler().sendMessage("fail-start-no-funds", player);
            } else if (auctionQueue.size() >= plugin.getConfig().getInt("auctionSettings.auctionQueueLimit", 3)) {
                // The Auction queue is full
                plugin.getMessageHandler().sendMessage("fail-start-queue-full", player);
            } else {
                try {
                    if (args.length >= 4) {
                        bidIncrement = Integer.parseInt(args[3]);

                        if (plugin.getConfig().getInt("auctionSettings.minimumBidIncrement", 10) > bidIncrement) {
                            plugin.getMessageHandler().sendMessage("fail-start-bid-increment", player);
                            return;
                        } else if (plugin.getConfig().getInt("auctionSettings.maximumBidIncrement", 9999) < bidIncrement) {
                            plugin.getMessageHandler().sendMessage("fail-start-bid-increment", player);
                            return;
                        }
                    }
                    if (args.length == 5) {
                        double autowinAmount = Integer.parseInt(args[4]); // Autowin
                        autoWin = plugin.getConfig().getBoolean("auctionSettings.canSpecifyAutowin", true) ? autowinAmount : -1;

                        // Check if the player is allowed to specify an autowin
                        if (!plugin.getConfig().getBoolean("auctionSettings.canSpecifyAutowin", true)) {
                            plugin.getMessageHandler().sendMessage("fail-start-no-autowin", player);
                            return;
                        }
                    }
                } catch (NumberFormatException exception) {
                    plugin.getMessageHandler().sendMessage("fail-number-format", player);
                    return;
                }

                // Decide whether to immediately start the auction or place it in the queue
                if (currentAuction != null && currentAuction.getOwner().equals(player.getUniqueId())) {
                    plugin.getMessageHandler().sendMessage("fail-start-already-auctioning", player);
                } else if (hasAuctionInQueue(player)) { // The player already has an auction queued
                    plugin.getMessageHandler().sendMessage("fail-start-already-queued", player);
                } else {
                    Auction auction = createAuction(plugin, player, numItems, startingPrice, bidIncrement, autoWin);

                    if (auction != null) {
                        if (currentAuction == null && this.canAuction) {
                            startAuction(auction);
                        } else {
                            auctionQueue.add(auction);
                            plugin.getMessageHandler().sendMessage("auction-queued", player);
                        }
                    }
                }
            }
        }
    } */

    /**
     * Starts an auction and withdraws the starting fee
     *
     * @param auction The auction to begin
     */
    /* public void startAuction(Auction auction) {
        if (auction == null) {
            return;
        }

        AuctionPlugin.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(auction.getOwner()), plugin.getConfig().getDouble("auctionSettings.startFee", 0));
        currentAuction = auction;
        auction.start();
        setCanAuction(false);
    } */

    /* public void cancelCurrentAuction(Player player) {
        if (currentAuction == null) {
            // No auction
            plugin.getMessageHandler().sendMessage("fail-cancel-no-auction", player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            // Ignoring
            plugin.getMessageHandler().sendMessage("fail-start-ignoring", player);
        } else if (!plugin.getConfig().getBoolean("allow-auction-cancel-command", true) && !player.hasPermission("auction.cancel.bypass")) {
            // Can't cancel
            plugin.getMessageHandler().sendMessage("fail-cancel-disabled", player);
        } else if (currentAuction.getTimeLeft() < plugin.getConfig().getInt("auctionSettings.mustCancelBefore", 15)
                && !player.hasPermission("auction.cancel.bypass")) {
            // Can't cancel
            plugin.getMessageHandler().sendMessage("fail-cancel-time", player);
        } else if (!currentAuction.getOwner().equals(player.getUniqueId()) && !player.hasPermission("auction.cancel.bypass")) {
            // Can't cancel other peoples auction
            plugin.getMessageHandler().sendMessage("fail-cancel-not-yours", player);
        } else {
            currentAuction.cancel();
        }
    } */

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
    /* public Auction createAuction(AuctionPlugin plugin, Player player, int numItems, double startingPrice, int bidIncrement, double autoWin) {
        Auction auction = null;
        try {
            auction = new Auction(AuctionPlugin.getPlugin(), player, numItems, startingPrice, bidIncrement, autoWin);
        } catch (NumberFormatException ex1) {
            plugin.getMessageHandler().sendMessage("fail-number-format", player);
        } catch (Exception ex2) {
            plugin.getMessageHandler().sendMessage(ex2.getMessage(), player);
        }

        if (auction != null && !player.hasPermission("auction.tax.exempt")) {
            auction.setTaxable(true);
        }

        return auction;
    } */

    /**
     * Formats String input provided by a player before proceeding to pre-bid
     * checking
     *
     * @param player The player bidding
     * @param amount The amount bid by the player
     */
    /* public void prepareBid(Player player, String amount) {
        try {
            double bid = Double.parseDouble(amount);
            prepareBid(player, bid);
        } catch (NumberFormatException ex) {
            plugin.getMessageHandler().sendMessage("fail-bid-number", player);
        }
    } */

    /**
     * Prepares a bid by a player and verifies they have met requirements before
     * bidding
     *
     * @param player The player bidding
     * @param amount The amount bid by the player
     */
    /* public void prepareBid(Player player, double amount) {
        if (currentAuction == null) {
            // There is no auction to bid on
            plugin.getMessageHandler().sendMessage("fail-bid-no-auction", player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            // Player is ignoring Auctions
            plugin.getMessageHandler().sendMessage("fail-start-ignoring", player);
        } else if (currentAuction.getOwner().equals(player.getUniqueId())) {
            // Players aren't allowed to bid on their own auction
            plugin.getMessageHandler().sendMessage("fail-bid-your-auction", player);
        } else if (amount < currentAuction.getTopBid() + currentAuction.getBidIncrement()) {
            // The player didn't bid enough
            plugin.getMessageHandler().sendMessage("fail-bid-too-low", player);
        } else if (AuctionPlugin.getEconomy().getBalance(player) < amount) {
            // The bid exceeds their balance
            plugin.getMessageHandler().sendMessage("fail-bid-insufficient-balance", player);
        } else if (currentAuction.getWinning() != null && currentAuction.getWinning().equals(player.getUniqueId())) {
            // The player already holds the highest bid
            plugin.getMessageHandler().sendMessage("fail-bid-top-bidder", player);
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
    } */

    /**
     * Places a bid on the current auction by the player
     *
     * @param player The player placing the bid
     * @param amount The amount bid by the player
     */
    /* public void placeBid(Player player, double amount) {
        currentAuction.setTopBid(amount);
        currentAuction.setWinning(player);
        AuctionPlugin.getEconomy().withdrawPlayer(player, amount);

        // Check if the auction isn't won due to autowin
        if (amount >= currentAuction.getAutoWin() && currentAuction.getAutoWin() != -1) {
            plugin.getMessageHandler().sendMessage(currentAuction, "auction-ended-autowin", false);
            currentAuction.end(true);
        } else {
            plugin.getMessageHandler().sendMessage(currentAuction, "bid-broadcast", false);

            if (currentAuction.getTimeLeft() <= plugin.getConfig().getInt("auctionSettings.antiSnipe.timeThreshold", 3)
                    && plugin.getConfig().getBoolean("auctionSettings.antiSnipe.enable", true)
                    && currentAuction.getAntiSniped() + 1 <= plugin.getConfig().getInt("auctionSettings.antiSnipe.maxPerAuction", 3)) {
                int time = plugin.getConfig().getInt("auctionSettings.antiSnipe.addSeconds", 5);
                if (time > 0) {
                    //plugin.getMessageHandler().sendMessage(currentAuction, "anti-snipe-add", false);
                    TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("anti-snipe-add").replace("%t", Integer.toString(time))), false, Bukkit.getOnlinePlayers());
                    currentAuction.addSeconds(time);
                    currentAuction.incrementAntiSniped();
                }
            }
        }
    } */

    /**
     * Called when a player ends an auction
     *
     * @param player The player who ended the auction
     */
    /* public void end(Player player) {
        if (currentAuction == null) {
            plugin.getMessageHandler().sendMessage("fail-end-no-auction", player);
        } else if (!plugin.getConfig().getBoolean("allow-auction-end-command", false) && !player.hasPermission("auction.end.bypass")) {
            plugin.getMessageHandler().sendMessage("fail-end-disallowed", player);
        } else if (!currentAuction.getOwner().equals(player.getUniqueId()) && !player.hasPermission("auction.end.bypass")) {
            plugin.getMessageHandler().sendMessage("fail-end-not-your-auction", player);
        } else {
            currentAuction.end(true);
            killAuction();
        }
    } */

    /**
     * Nulls the auction
     */
    /* public void killAuction() {
        currentAuction = null;
    } */

    /* 
     * Loads all banned items into memory 
     */
    private void loadBannedMaterials() {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        if (!plugin.getConfig().isList("general.blockedMaterials")) {
            return;
        }

        for (String materialString : plugin.getConfig().getStringList("general.blockedMaterials")) {
            Material material = Material.getMaterial(materialString);

            if (material == null) {
                plugin.getLogger().info("Material \"" + materialString + "\" is not a valid Material and will not be blocked.");
            } else {
                banned.add(material);
                plugin.getLogger().info("Material \"" + material.toString() + "\" added as a blocked material.");
            }
        }
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
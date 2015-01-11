package me.sainttx.auction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class AuctionManager implements Listener {

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
    private Queue<Auction> auctionQueue = new ArrayDeque<Auction>();

    /*
     * Banned materials in an auction
     */
    private static ArrayList<Material> banned = new ArrayList<Material>();

    /*
     * Information on whether an auction can be started
     */
    private boolean disabled    = false;
    private boolean canAuction  = true;

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
     * Returns whether or not a player has an auction queued
     * 
     * @param p A player who may have an auction queued
     * 
     * @return True if the player has an auction queued, false otherwise
     */
    public static boolean hasAuctionQueued(Player p) {
        for (Auction queued : manager.auctionQueue) {
            if (queued.getOwner().equals(p.getUniqueId())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns whether or not a player is participating in an auction
     * 
     * @param p A player who may be participating in an auction
     * 
     * @return True if the player is the owner of the current auction or if 
     *         the player has an active bid on the current auction
     */
    public static boolean isAuctionParticipant(Player p) {
        return currentAuction == null ? false : currentAuction.getOwner().equals(p.getUniqueId()) || currentAuction.getWinning().equals(p.getUniqueId());
    }

    /**
     * Gives information about the auction to a Player
     * 
     * @param player The player to receive auction information
     */
    public void sendAuctionInfo(Player player) {
        if (currentAuction != null) {
            TextUtil.sendMessage(TextUtil.replace(currentAuction, TextUtil.getConfigMessage("auction-info-message")), player);
        } else {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-info-no-auction"), player);
        }
    }

    /**
     * Performs pre-checks for creating an auction started by a player
     * 
     * @param player The player who started the auction
     * @param args   Arguments relative to the auction provided by the player
     */
    public void prepareAuction(Player player, String[] args) {
        double minStartingPrice = plugin.getConfig().getDouble("min-start-price", 0);
        double maxStartingPrice = plugin.getConfig().getDouble("max-start-price", Integer.MAX_VALUE);

        // Check if auctioning is allowed
        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-auction-disabled"), player);
        }

        // Check if the player can bypass the start delay
        else if (!canAuction && !player.hasPermission("auction.bypass.startdelay")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-cant-yet"), player);
        }

        // else if (currentAuction != null) {
        //     messager.sendText(player, "fail-start-auction-in-progress", true);
        // }

        // Check if the player provided the minimum amount of arguments
        else if (args.length < 3) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-syntax"), player);
        }

        else {
            int numItems = -1;
            double startingPrice = -1;
            double autoWin = -1;
            double fee = plugin.getConfig().getDouble("auction-start-fee", 0);

            try {
                numItems = Integer.parseInt(args[1]);
                startingPrice = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-number-format"), player);
            }

            // Check if the player has provided a positive amount of items
            if (numItems < 0) {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-negative-number"), player);
            }

            // Check if the player has specified a correct starting price (lower bound)
            else if (startingPrice < minStartingPrice) {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-min"), player);
            }

            // Check if the player has specified a correct starting price (upper bound)
            else if (startingPrice > maxStartingPrice) {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-max"), player);
            }

            // Check if the player has enough money
            else if (fee > AuctionPlugin.getEconomy().getBalance(player)) {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-no-funds"), player);
            }

            // Check if the queue is full
            else if (auctionQueue.size() > 5) {
                // TODO: Message that the queue is full
            }

            else {
                if (args.length == 4) {
                    double autowinAmount = Integer.parseInt(args[3]); // Autowin
                    autoWin = plugin.getConfig().getBoolean("allow-autowin", false) ? autowinAmount : -1;

                    // Check if the player is allowed to specify an autowin
                    if (!plugin.getConfig().getBoolean("allow-autowin", false)) {
                        TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-no-autowin"), player);
                    }
                }

                Auction auction = createAuction(plugin, player, numItems, startingPrice, autoWin);

                // Decide whether to immediately start the auction or place it in the queue
                if (currentAuction == null && this.canAuction) {
                    startAuction(auction);
                } else if (hasAuctionQueued(player)) {
                    // TODO: Msg saying they already have an auction queued
                } else {
                    auctionQueue.add(auction);
                    // TODO: Msg saying their auction was added to the queue
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

    /**
     * Creates an auction and verifies it was properly specified
     * 
     * @param plugin        The Auction plugin
     * @param player        The player starting the auction
     * @param numItems      The number of items the player is auctioning
     * @param startingPrice The starting price of the auction
     * @param autoWin       The amount required to bid to automatically win
     * 
     * @return The auction result, null if something went wrong
     */
    public Auction createAuction(AuctionPlugin plugin, Player player, int numItems, double startingPrice, double autoWin) {
        Auction auction = null;
        try {
            auction = new Auction(AuctionPlugin.getPlugin(), player, numItems, startingPrice, autoWin);
        } catch (NumberFormatException ex1) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-number-format"), player);
        } catch (Exception ex2) {
            TextUtil.sendMessage(TextUtil.getConfigMessage(ex2.getMessage()), player);
        }

        if (!player.hasPermission("auction.tax.exempt")) {
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
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-number"), player);
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
        // Check if there's an auction to bid on
        if (currentAuction == null) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-no-auction"), player);
        }

        // Check if the auction isn't their own
        else if (currentAuction.getOwner().equals(player.getUniqueId())) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-your-auction"), player);
        }

        // Check if they bid enough
        else if (amount < currentAuction.getTopBid() + plugin.getConfig().getDouble("minimum-bid-increment", 1D)) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-too-low"), player);
        }

        // Check if they have enough money to bid
        else if (AuctionPlugin.getEconomy().getBalance(player) < amount) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-insufficient-balance"), player);
        }

        // Check if they're already the top bidder
        else if (currentAuction.getWinning() != null && currentAuction.getWinning().equals(player.getUniqueId())) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-top-bidder"), player);
        }

        else {
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
        currentAuction.setWinning(player.getUniqueId());
        AuctionPlugin.getEconomy().withdrawPlayer(player, amount);

        // Check if the auction isn't won due to autowin
        if (amount >= currentAuction.getAutoWin() && currentAuction.getAutoWin() != -1) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("auction-ended-autowin"), Bukkit.getOnlinePlayers().toArray(new Player[0]));
            currentAuction.end(true);
        } else {
            TextUtil.sendMessage(TextUtil.getConfigMessage("bid-broadcast"), Bukkit.getOnlinePlayers().toArray(new Player[0]));
        }
    }

    /**
     * Called when a player ends an auction
     * 
     * @param player The player who ended the auction
     */
    public void end(Player player) {
        if (currentAuction == null) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-end-no-auction"), player);
        } else if (!plugin.getConfig().getBoolean("allow-end", false) && !player.hasPermission("auction.end.bypass")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-end-disallowed"), player);
        } else if (!currentAuction.getOwner().equals(player.getUniqueId()) && !player.hasPermission("auction.end.bypass")) {
            // TODO: Can't end other players auction
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
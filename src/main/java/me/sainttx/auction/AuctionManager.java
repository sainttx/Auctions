package java.me.sainttx.auction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class AuctionManager {

    private static AuctionManager manager;
    private static AuctionPlugin plugin;
    private Messages messager;

    private static @Getter Auction currentAuction;
    private Queue<Auction> auctionQueue = new ArrayDeque<Auction>();

    private static ArrayList<Material> banned = new ArrayList<Material>();

    private @Getter @Setter boolean disabled    = false;
    private @Getter @Setter boolean canAuction  = true;

    /**
     * Creates the Auction Manager
     */
    private AuctionManager() {
        plugin = AuctionPlugin.getPlugin();
        messager = Messages.getMessager();
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
            messager.sendText(player, currentAuction, "auction-info-message", true);
        } else {
            messager.sendText(player, "fail-info-no-auction", true);    
        }
    }

    /**
     * Performs pre-checks for creating an auction started by a player
     * 
     * @param player The player who started the auction
     * @param args   Arguments relative to the auction provided by the player
     */
    public void prepareAuction(Player player, String[] args) {
        Messages messager = Messages.getMessager();
        double minStartingPrice = plugin.getMinimumStartPrice();
        double maxStartingPrice = plugin.getMaxiumumStartPrice();

        // Check if auctioning is allowed
        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            messager.sendText(player, "fail-start-auction-disabled", true);
        }

        // Check if the player can bypass the start delay
        else if (!canAuction && !player.hasPermission("auction.bypass.startdelay")) {
            messager.sendText(player, "fail-start-cant-yet", true);
        }

        // else if (currentAuction != null) {
        //     messager.sendText(player, "fail-start-auction-in-progress", true);
        // }

        // Check if the player provided the minimum amount of arguments
        else if (args.length < 3) {
            messager.sendText(player, "fail-start-syntax", true);
        }

        else {
            int numItems = -1;
            double startingPrice = -1;
            double autoWin = -1;
            double fee = plugin.getStartFee();

            try {
                numItems = Integer.parseInt(args[1]);
                startingPrice = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                messager.sendText(player, "fail-number-format", true);
            }

            // Check if the player has provided a positive amount of items
            if (numItems < 0) {
                messager.sendText(player, "fail-start-negative-number", true);
            }

            // Check if the player has specified a correct starting price (lower bound)
            else if (startingPrice < minStartingPrice) {
                messager.sendText(player, "fail-start-min", true);
            }

            // Check if the player has specified a correct starting price (upper bound)
            else if (startingPrice > maxStartingPrice) {
                messager.sendText(player, "fail-start-max", true);
            }

            // Check if the player has enough money
            else if (fee > AuctionPlugin.economy.getBalance(player)) { 
                messager.sendText(player, "fail-start-no-funds", true);
            }

            // Check if the queue is full
            else if (auctionQueue.size() > 5) {
                // TODO: Message that the queue is full
            }

            else {
                if (args.length == 4) {
                    double aw = Integer.parseInt(args[3]); // Autowin
                    autoWin = plugin.isAllowAutowin() ? aw : -1;

                    // Check if the player is allowed to specify an autowin
                    if (!plugin.isAllowAutowin()) {
                        messager.sendText(player, "fail-start-no-autowin", true);
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

        AuctionPlugin.economy.withdrawPlayer(Bukkit.getOfflinePlayer(auction.getOwner()), plugin.getStartFee());
        auction.start();
        setCanAuction(false);
        currentAuction = auction;
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
            messager.sendText(player, "fail-number-format", true);
        } catch (Exception ex2) {
            messager.sendText(player, ex2.getMessage(), true);
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
            messager.sendText(player, "fail-bid-number", true);
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
            messager.sendText(player, "fail-bid-no-auction", true);
        }

        // Check if the auction isn't their own
        else if (currentAuction.getOwner().equals(player.getUniqueId())) {
            messager.sendText(player, "fail-bid-your-auction", true);
        }

        // Check if they bid enough
        else if (amount < currentAuction.getTopBid() + plugin.getMinBidIncrement()) { 
            messager.sendText(player, "fail-bid-too-low", true);
        }

        // Check if they have enough money to bid
        else if (plugin.economy.getBalance(player) < amount) {
            messager.sendText(player, "fail-bid-insufficient-balance", true);
        }

        // Check if they're already the top bidder
        else if (currentAuction.getWinning() != null && currentAuction.getWinning().equals(player.getUniqueId())) {
            messager.sendText(player, "fail-bid-top-bidder", true);
        }

        else {
            if (currentAuction.getWinning() != null) {
                Player oldWinner = Bukkit.getPlayer(currentAuction.getWinning());
                if (oldWinner != null) {
                    AuctionPlugin.economy.depositPlayer(oldWinner, currentAuction.getTopBid());
                } else {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(currentAuction.getWinning());
                    AuctionPlugin.economy.depositPlayer(offline, currentAuction.getTopBid());
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
        AuctionPlugin.economy.withdrawPlayer(player, amount);

        // Check if the auction isn't won due to autowin
        if (amount >= currentAuction.getAutoWin() && currentAuction.getAutoWin() != -1) {
            messager.messageListeningAll(currentAuction, "auction-ended-autowin", true);
            currentAuction.end(true);
            return;
        }

        messager.messageListeningAll(currentAuction, "bid-broadcast", true);
    }

    /**
     * Called when a player ends an auction
     * 
     * @param player The player who ended the auction
     */
    public void end(Player player) {
        if (currentAuction == null) {
            Messages.getMessager().sendText(player, "fail-end-no-auction", true);    
        } else if (!plugin.isAllowEnding() && !player.hasPermission("auction.end.bypass")) {
            Messages.getMessager().sendText(player, "fail-end-disallowed", true);
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
}
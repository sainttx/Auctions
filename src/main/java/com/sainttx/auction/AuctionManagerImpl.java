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

    /**
     * Called when the Auctions plugin disables
     */
    protected static void disable() {
        if (getAuctionManager().getCurrentAuction() != null) {
            getAuctionManager().getCurrentAuction().cancel();
        }

        for (Auction auction : getAuctionManager().getQueue()) {
            auction.end(false);
        }
        getAuctionManager().getQueue().clear();

        manager = null;
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
    public void setCurrentAuction(Auction auction) {
        this.currentAuction = auction;
    }

    @Override
    public boolean canStartNewAuction() {
        return canAuction;
    }

    @Override
    public void setCanStartNewAuction(boolean start) {
        this.canAuction = start;
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
    public MessageHandler getMessageHandler() {
        return null;
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
            currentAuction = next;
        }
    }

    @Override
    public boolean isBannedMaterial(Material material) {
        return false;
    }

    /**
     * Performs pre-checks for creating an auction started by a player
     *
     * @param player The player who started the auction
     * @param args   Arguments relative to the auction provided by the player
     */
    /* public void prepareAuction(Player player, String[] args) {
        double minStartingPrice = plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0);
        double maxStartingPrice = plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999);

        else {
            int numItems;
            double startingPrice;
            int bidIncrement = plugin.getConfig().getInt("auctionSettings.defaultBidIncrement", 50);
            double autoWin = -1;
            double fee =



                 else {
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
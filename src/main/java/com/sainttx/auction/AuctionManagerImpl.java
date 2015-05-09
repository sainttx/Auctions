package com.sainttx.auction;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.messages.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuctionManagerImpl implements AuctionManager {

    // Instance
    private static AuctionManagerImpl manager = new AuctionManagerImpl();

    // Auctions information
    private Auction currentAuction;
    private Queue<Auction> auctionQueue = new ConcurrentLinkedQueue<Auction>();
    private Set<Material> banned = EnumSet.noneOf(Material.class);
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
        return currentAuction != null && canAuction;
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
        return banned.contains(material);
    }

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
}
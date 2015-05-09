package com.sainttx.auction.structure;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionType;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.module.AuctionModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * An auction implementation
 */
public abstract class AbstractAuction implements Auction {

    // Instance
    protected AuctionPlugin plugin;
    protected AuctionType type;
    protected Collection<AuctionModule> modules;

    // Auction owner information
    protected UUID ownerUUID;
    protected String ownerName;

    // Top bidder information
    protected UUID topBidderUUID;
    protected String topBidderName;
    protected double winningBid;

    // Auction information
    protected ItemStack auctionedItem;
    protected double bidIncrement;
    protected int timeLeft;
    protected BukkitTask timerTask;

    /*
     * Protect from reflective instantiation
     */
    private AbstractAuction() {
        throw new IllegalAccessError("cannot create empty auction instances");
    }

    /**
     * Creates an Auction
     *
     * @param plugin the auction plugin instance
     * @param type   the specified auction type
     */
    AbstractAuction(AuctionPlugin plugin, AuctionType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public UUID getOwner() {
        return ownerUUID;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public UUID getTopBidder() {
        return topBidderUUID;
    }

    @Override
    public String getTopBidderName() {
        return topBidderName;
    }

    @Override
    public ItemStack getItem() {
        return auctionedItem;
    }

    @Override
    public AuctionType getType() {
        return type;
    }

    @Override
    public double getTopBid() {
        return winningBid;
    }

    @Override
    public void placeBid(Player player, double bid) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        this.winningBid = bid;
        this.topBidderName = player.getName();
        this.topBidderUUID = player.getUniqueId();

        // Trigger our modules
        for (AuctionModule module : modules) {
            if (module.canTrigger()) {
                module.trigger();
            }
        }
    }

    @Override
    public int getTimeLeft() {
        return timeLeft;
    }

    @Override
    public void setTimeLeft(int time) {
        this.timeLeft = time;
    }

    @Override
    public void start() {
        this.timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new AuctionTimer(), 20L, 20L);
        startMessages();
    }

    /**
     * Dispatches messages for the start of the auction
     */
    protected void startMessages() {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        manager.getMessageHandler().sendMessage(this, "auction-start", false);
        manager.getMessageHandler().sendMessage(this, "auction-start-price", false);
        manager.getMessageHandler().sendMessage(this, "auction-start-increment", false);
    }

    @Override
    public void cancel() {

    }

    @Override
    public void end(boolean broadcast) {

    }

    @Override
    public double getBidIncrement() {
        return bidIncrement;
    }

    @Override
    public double getTax() {
        return plugin.getConfig().getInt("auctionSettings.taxPercent", 0);
    }

    @Override
    public Collection<AuctionModule> getModules() {
        return new HashSet<AuctionModule>(modules);
    }

    @Override
    public void addModule(AuctionModule module) {
        if (module == null) {
            throw new IllegalArgumentException("module cannot be null");
        }

        this.modules.add(module);
    }

    @Override
    public boolean removeModule(AuctionModule module) {
        return this.modules.remove(module);
    }

    /**
     * An implementation of an auction timer
     */
    public class AuctionTimer implements Auction.Timer {

        @Override
        public void run() {
            timeLeft--;

            if (timeLeft <= 0) {
                end(true);
            } // TODO: Check if the timer should broadcast
        }
    }

    /**
     * An implementation of an Auction builder for auctions
     */
    public static abstract class AbstractAuctionBuilder implements Builder {

        protected AuctionPlugin plugin;
        protected double increment = -1;
        protected int time = -1;
        protected ItemStack item;
        protected double bid = -1;
        protected UUID ownerId;
        protected String ownerName;

        public AbstractAuctionBuilder(AuctionPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public Builder bidIncrement(double increment) {
            this.increment = increment;
            return this;
        }

        @Override
        public Builder owner(Player owner) {
            this.ownerId = owner.getUniqueId();
            this.ownerName = owner.getName();
            return this;
        }

        @Override
        public Builder time(int time) {
            this.time = time;
            return this;
        }

        @Override
        public Builder item(ItemStack item) {
            this.item = item;
            return this;
        }

        @Override
        public Builder topBid(double bid) {
            this.bid = bid;
            return this;
        }

        /**
         * Initializes any default values that haven't been set
         */
        protected void defaults() {
            if (item == null) {
                throw new IllegalStateException("item cannot be null");
            } else if (bid == -1) {
                throw new IllegalStateException("bid hasn't been set");
            } else if (increment == -1) {
                increment = plugin.getConfig().getInt("auctionSettings.defaultBidIncrement", 50);
            } else if (time == -1) {
                time = plugin.getConfig().getInt("auctionSettings.startTime", 30);
            }
        }
    }
}

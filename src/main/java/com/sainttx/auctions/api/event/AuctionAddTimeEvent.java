package com.sainttx.auctions.api.event;

import com.sainttx.auctions.api.Auction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Created by Matthew on 9/7/2015.
 */
public class AuctionAddTimeEvent extends AuctionEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private int secondsToAdd;

    /**
     * Created when an anti snipe module is attempting to add time to an auction
     *
     * @param auction      the affected auction
     * @param secondsToAdd the number of seconds to add
     */
    public AuctionAddTimeEvent(final Auction auction, int secondsToAdd) {
        super(auction);
        this.secondsToAdd = secondsToAdd;
    }

    /**
     * Returns the amount of seconds that are being added to the auction
     *
     * @return extra time to be added
     */
    public int getSecondsToAdd() {
        return secondsToAdd;
    }

    /**
     * Sets the amount of seconds to be added to the auction
     *
     * @param secondsToAdd the new extra time to be added
     */
    public void setSecondsToAdd(int secondsToAdd) {
        this.secondsToAdd = secondsToAdd;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
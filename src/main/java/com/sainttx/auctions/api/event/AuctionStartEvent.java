package com.sainttx.auctions.api.event;

import com.sainttx.auctions.api.Auction;
import org.bukkit.event.HandlerList;

/**
 * Created by Matthew on 9/7/2015.
 */
public class AuctionStartEvent extends AuctionEvent {

    private static final HandlerList handlers = new HandlerList();

    /**
     * Called when an auction starts
     *
     * @param auction the affected auction
     */
    public AuctionStartEvent(final Auction auction) {
        super(auction);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

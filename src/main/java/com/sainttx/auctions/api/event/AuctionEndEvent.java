package com.sainttx.auctions.api.event;

import com.sainttx.auctions.api.Auction;
import org.bukkit.event.HandlerList;

/**
 * Created by Matthew on 9/7/2015.
 */
public class AuctionEndEvent extends AuctionEvent {

    private static final HandlerList handlers = new HandlerList();

    /**
     * Called when an auction ends
     *
     * @param auction the auction that ends
     */
    public AuctionEndEvent(final Auction auction) {
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
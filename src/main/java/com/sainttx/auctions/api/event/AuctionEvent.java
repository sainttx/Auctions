package com.sainttx.auctions.api.event;

import com.sainttx.auctions.api.Auction;
import org.bukkit.event.Event;

/**
 * Created by Matthew on 9/7/2015.
 */
public abstract class AuctionEvent extends Event {

    protected Auction auction;

    public AuctionEvent(final Auction auction) {
        this.auction = auction;
    }

    public final Auction getAuction() {
        return auction;
    }
}

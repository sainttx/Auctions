package com.sainttx.auction.structure.module;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.module.AuctionModule;

/**
 * Created by Matthew on 08/05/2015.
 */
public class AutoWinModule implements AuctionModule {

    private AuctionPlugin plugin = AuctionPlugin.getPlugin();
    private Auction auction;
    private double trigger;

    public AutoWinModule(Auction auction, double trigger) {
        if (auction == null) {
            throw new IllegalArgumentException("auction cannot be null");
        }
        this.auction = auction;
        this.trigger = trigger;
    }

    @Override
    public boolean canTrigger() {
        return auction.getTopBid() >= trigger;
    }

    @Override
    public void trigger() {
        auction.end(true);
    }
}

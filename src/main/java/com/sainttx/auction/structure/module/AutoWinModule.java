package com.sainttx.auction.structure.module;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.module.AuctionModule;

/**
 * A module that ends an auction when a bid higher than
 * the autowin amount is placed on an auction.
 */
public class AutoWinModule implements AuctionModule {

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
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        AuctionsAPI.getMessageHandler().broadcast(auction, plugin.getMessage("messages.auctionFormattable.endByAutowin"), false);
        auction.end(true);
    }
}

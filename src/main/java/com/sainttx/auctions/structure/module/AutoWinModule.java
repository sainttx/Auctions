package com.sainttx.auctions.structure.module;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.module.AuctionModule;

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
        AuctionsAPI.getMessageHandler().broadcast(plugin.getMessage("messages.auctionFormattable.endByAutowin"), auction, false);
        auction.end(true);
    }
}

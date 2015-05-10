package com.sainttx.auction.structure;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionType;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.reward.Reward;

import java.util.UUID;

/**
 * A standard auction implementation
 */
public class StandardAuction extends AbstractAuction {

    /**
     * Creates an Auction
     *
     * @param plugin the auction plugin instance
     */
    private StandardAuction(AuctionPlugin plugin, UUID ownerUUID, String ownerName,
                            double topBid, Reward reward, double autowin, double bidIncrement, int timeLeft) {
        super(plugin, AuctionType.STANDARD);
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.winningBid = topBid;
        this.reward = reward;
        this.autowin = autowin;
        this.bidIncrement = bidIncrement;
        this.timeLeft = timeLeft;
    }

    @Override
    public void broadcastBid() {
        AuctionsAPI.getMessageHandler().broadcast(plugin.getMessage("messages.auctionFormattable.bid"), this, false);
    }

    /**
     * An implementation of an Auction builder for standard auctions
     */
    public static class StandardAuctionBuilder extends AbstractAuctionBuilder {

        public StandardAuctionBuilder(AuctionPlugin plugin) {
            super(plugin);
        }

        @Override
        public Auction build() {
            super.defaults();
            return new StandardAuction(this.plugin, this.ownerId, this.ownerName,
                    this.bid, this.reward, this.autowin, this.increment, this.time);
        }
    }
}

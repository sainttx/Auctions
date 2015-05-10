package com.sainttx.auction.structure;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionType;
import com.sainttx.auction.api.reward.Reward;

import java.util.UUID;

/**
 * An auction that will not broadcast any bids
 */
public class SilentAuction extends AbstractAuction {

    /**
     * Creates an Auction
     *
     * @param plugin the auction plugin instance
     */
    private SilentAuction(AuctionPlugin plugin, UUID ownerUUID, String ownerName,
                          double topBid, Reward reward, double autowin, double bidIncrement, int timeLeft) {
        super(plugin, AuctionType.SILENT);
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.winningBid = topBid;
        this.reward = reward;
        this.autowin = autowin;
        this.bidIncrement = bidIncrement;
        this.timeLeft = timeLeft;
    }

    /**
     * An implementation of an Auction builder for silent auctions
     */
    public static class SilentAuctionBuilder extends AbstractAuctionBuilder {

        public SilentAuctionBuilder(AuctionPlugin plugin) {
            super(plugin);
        }

        @Override
        public Auction build() {
            super.defaults();
            return new SilentAuction(this.plugin, this.ownerId, this.ownerName,
                    this.bid, this.reward, this.autowin, this.increment, this.time);
        }
    }
}

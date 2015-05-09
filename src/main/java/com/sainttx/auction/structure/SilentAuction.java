package com.sainttx.auction.structure;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionType;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Created by Matthew on 08/05/2015.
 */
public class SilentAuction extends AbstractAuction {

    /**
     * Creates an Auction
     *
     * @param plugin the auction plugin instance
     */
    private SilentAuction(AuctionPlugin plugin, UUID ownerUUID, String ownerName,
                          double topBid, ItemStack item, double autowin, double bidIncrement, int timeLeft) {
        super(plugin, AuctionType.SILENT);
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.winningBid = topBid;
        this.auctionedItem = item;
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
                    this.bid, this.item, this.autowin, this.increment, this.time);
        }
    }
}

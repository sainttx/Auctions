package com.sainttx.auction.api;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.structure.SilentAuction;
import com.sainttx.auction.structure.StandardAuction;

/**
 * A central API to handle all external Auction plugin needs
 */
public class AuctionsAPI {

    /**
     * Gets the Auction Manager instance
     *
     * @return the auction manager
     */
    public static AuctionManager getAuctionManager() {
        return null;
    }

    /**
     * Gets an auction builder for a specific auction type
     *
     * @param type the type of auction
     * @return an auction builder for the specific auction type
     */
    public static Auction.Builder getAuctionBuilder(AuctionType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }

        switch (type) {
            case STANDARD:
                return new StandardAuction.StandardAuctionBuilder(AuctionPlugin.getPlugin());
            case SILENT:
                return new SilentAuction.SilentAuctionBuilder(AuctionPlugin.getPlugin());
            default:
                return null;
        }
    }
}

package com.sainttx.auction.api;

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
        return null;
    }
}

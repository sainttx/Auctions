package com.sainttx.auctions.api;

import com.sainttx.auctions.AuctionManagerImpl;
import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.structure.SealedAuction;
import com.sainttx.auctions.structure.StandardAuction;

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
        return AuctionManagerImpl.getAuctionManager();
    }

    /**
     * Gets the MessageHandler of {@link #getAuctionManager()}
     *
     * @return the message handler
     */
    public static MessageHandler getMessageHandler() {
        return getAuctionManager().getMessageHandler();
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
            case SEALED:
                return new SealedAuction.SealedAuctionBuilder(AuctionPlugin.getPlugin());
            default:
                return null;
        }
    }
}

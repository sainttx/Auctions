/*
 * Copyright (C) SainttX <http://sainttx.com>
 * Copyright (C) contributors
 *
 * This file is part of Auctions.
 *
 * Auctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Auctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Auctions.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sainttx.auctions.api;

import com.sainttx.auctions.AuctionManagerImpl;
import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.structure.auction.SealedAuction;
import com.sainttx.auctions.structure.auction.StandardAuction;

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

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

package com.sainttx.auctions.structure.auction;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.structure.DefaultAuction;

import java.util.UUID;

/**
 * A standard auction implementation
 */
public class StandardAuction extends DefaultAuction {

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
        this.startPrice = topBid;
        this.reward = reward;
        this.autowin = autowin;
        this.bidIncrement = bidIncrement;
        this.timeLeft = timeLeft;
    }

    /**
     * An implementation of an Auction builder for standard auctions
     */
    public static class StandardAuctionBuilder extends DefaultAuctionBuilder {

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

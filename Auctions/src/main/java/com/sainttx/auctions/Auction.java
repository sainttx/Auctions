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

package com.sainttx.auctions;

import com.sainttx.auctions.api.reward.Reward;

import java.util.UUID;

public class Auction implements com.sainttx.auctions.api.Auction {

    private UUID owner;
    private UUID bidder;
    private String ownerName;
    private String bidderName;
    private Reward reward;
    private double startPrice;
    private double bidIncrement;
    private double autowin;
    private double bid;

    public Auction(UUID owner, String ownerName, UUID bidder, String bidderName, Reward reward, double startPrice,
                   double bidIncrement) {
        this(owner, ownerName, bidder, bidderName, reward, startPrice, bidIncrement, -1D);
    }

    public Auction(UUID owner, String ownerName, UUID bidder, String bidderName, Reward reward, double startPrice,
                   double bidIncrement, double autowin) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.bidder = bidder;
        this.bidderName = bidderName;
        this.reward = reward;
        this.startPrice = startPrice;
        this.bidIncrement = bidIncrement;
        this.autowin = autowin;
        this.bid = com.sainttx.auctions.api.Auction.NO_BID;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public UUID getBidder() {
        return bidder;
    }

    @Override
    public String getBidderName() {
        return bidderName;
    }

    @Override
    public Reward getReward() {
        return reward;
    }

    @Override
    public double getStartPrice() {
        return startPrice;
    }

    @Override
    public double getBidIncrement() {
        return bidIncrement;
    }

    @Override
    public double getAutowin() {
        return autowin;
    }

    @Override
    public double getBid() {
        return bid;
    }

    @Override
    public void setBid(double bid) {
        this.bid = bid;
    }
}

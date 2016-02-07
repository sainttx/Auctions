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

import com.sainttx.auctions.api.reward.Reward;

import java.util.UUID;

/**
 * Represents an Auction that players can bid on
 */
public interface Auction {

    /**
     * The return value of {@link #getBid()} when no bids have been placed.
     */
    double NO_BID = -1;

    /**
     * Gets the owner of this auction
     *
     * @return the auction creator
     */
    UUID getOwner();

    /**
     * Gets the name of the owner of this auction
     *
     * @return the auction creators name
     */
    String getOwnerName();

    /**
     * Gets the {@link UUID} of the current top bidder for this auction
     *
     * @return the current {@link UUID} of the winner
     */
    UUID getBidder();

    /**
     * Gets the name of the current top bidder
     *
     * @return the top bidders name
     */
    String getBidderName();

    /**
     * Gets the reward that is being auctioned
     *
     * @return this auctions reward
     */
    Reward getReward();

    /**
     * Gets the starting price of this auction
     *
     * @return the starting price
     */
    double getStartPrice();

    /**
     * Gets the lowest amount that can be bid on this auction
     *
     * @return the bid increment of this auction
     */
    double getBidIncrement();

    /**
     * Returns the auctions autowin. Returns -1 if autowin was not set.
     *
     * @return how much money is required to automatically win the auction
     */
    double getAutowin();

    /**
     * Returns the current bid on this Auction. This method will return {@link #NO_BID}
     * when no bid has been placed yet.
     *
     * @return the current bid by a user
     */
    double getBid();

    /**
     * Sets the bid in this auction. This bid must exceed the current value
     * provided by {@link #getBid()} + {@link #getBidIncrement()}.
     *
     * @param bid the new bid
     */
    void setBid(double bid);
}

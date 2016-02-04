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

package com.sainttx.auctions.structure.module;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.module.AuctionModule;

/**
 * A module that ends an auction when a bid higher than
 * the autowin amount is placed on an auction.
 */
public class AutoWinModule implements AuctionModule {

    private AuctionPlugin plugin;
    private Auction auction;
    private double trigger;

    public AutoWinModule(AuctionPlugin plugin, Auction auction, double trigger) {
        if (auction == null) {
            throw new IllegalArgumentException("auction cannot be null");
        }
        this.plugin = plugin;
        this.auction = auction;
        this.trigger = trigger;
    }

    @Override
    public boolean canTrigger() {
        return auction.getTopBid() >= trigger;
    }

    @Override
    public void trigger() {
        plugin.getMessageHandler().broadcast(plugin.getMessage("messages.auctionFormattable.endByAutowin"), auction, false);
        auction.end(true);
    }
}

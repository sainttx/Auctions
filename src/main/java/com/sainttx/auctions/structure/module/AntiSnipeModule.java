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
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.module.AuctionModule;
import com.sainttx.auctions.util.TimeUtil;

/**
 * A module that ends adds time to an auction iff the auction
 * is within the time threshold set in configuration, and the
 * current anti snipe count is less than the maximum per
 * auction set in configuration.
 */
public class AntiSnipeModule implements AuctionModule {

    private AuctionPlugin plugin = AuctionPlugin.getPlugin();
    private Auction auction;
    private int snipeCount; // how many times the auction has been sniped

    public AntiSnipeModule(Auction auction) {
        if (auction == null) {
            throw new IllegalArgumentException("auction cannot be null");
        }
        this.auction = auction;
    }

    @Override
    public boolean canTrigger() {
        return auction.getTimeLeft() <= plugin.getConfig().getInt("auctionSettings.antiSnipe.timeThreshold", 3)
                && snipeCount < plugin.getConfig().getInt("auctionSettings.antiSnipe.maxPerAuction", 3)
                && (auction.getAutowin() == -1 || auction.getTopBid() < auction.getAutowin());
    }

    @Override
    public void trigger() {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        snipeCount++;
        int secondsToAdd = plugin.getConfig().getInt("auctionSettings.antiSnipe.addSeconds", 5);

        auction.setTimeLeft(auction.getTimeLeft() + secondsToAdd);
        String message = plugin.getMessage("messages.auctionFormattable.antiSnipeAdd")
                .replace("[snipetime]", TimeUtil.getFormattedTime(secondsToAdd));
        manager.getMessageHandler().broadcast(message, auction, false);
    }
}

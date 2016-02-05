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

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.event.AuctionAddTimeEvent;
import com.sainttx.auctions.api.module.AuctionModule;
import com.sainttx.auctions.util.TimeUtil;
import org.bukkit.Bukkit;

/**
 * A module that ends adds time to an auction iff the auction
 * is within the time threshold set in configuration, and the
 * current anti snipe count is less than the maximum per
 * auction set in configuration.
 */
public class AntiSnipeModule implements AuctionModule {

    private AuctionPlugin plugin;
    private Auction auction;
    private int snipeCount; // how many times the auction has been sniped

    public AntiSnipeModule(AuctionPlugin plugin, Auction auction) {
        if (auction == null) {
            throw new IllegalArgumentException("auction cannot be null");
        }

        this.plugin = plugin;
        this.auction = auction;
    }

    @Override
    public boolean canTrigger() {
        return plugin.getSettings().isAntiSnipeEnabled()
                && auction.getTimeLeft() <= plugin.getSettings().getAntiSnipeTimeThreshold()
                && snipeCount < plugin.getSettings().getMaximumAntiSnipesPerAuction()
                && (auction.getAutowin() == -1 || auction.getTopBid() < auction.getAutowin());
    }

    @Override
    public void trigger() {
        AuctionAddTimeEvent event = new AuctionAddTimeEvent(auction, plugin.getSettings().getAntiSnipeExtraTime());
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        snipeCount++;
        auction.setTimeLeft(auction.getTimeLeft() + event.getSecondsToAdd());
        String message = plugin.getMessage("messages.auctionFormattable.antiSnipeAdd")
                .replace("[snipetime]", TimeUtil.getFormattedTime(event.getSecondsToAdd(),
                        plugin.getSettings().shouldUseShortenedTimes()));
        plugin.getManager().getMessageHandler().broadcast(message, auction, false);
    }
}

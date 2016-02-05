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

package com.sainttx.auctions.hook;

import com.sainttx.auctions.AuctionPluginImpl;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.util.TimeUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import org.bukkit.entity.Player;

/**
 * Fomratting for extended_clip's PlaceholderAPI
 */
public class PlaceholderAPIHook {

    /**
     * Registers all placeholders for the auction plugin
     */
    public static void registerPlaceHolders(final AuctionPluginImpl plugin) {
        PlaceholderAPI.registerPlaceholderHook(plugin, new PlaceholderHook() {
            @Override
            public String onPlaceholderRequest(Player player, String token) {
                Auction current = plugin.getManager().getCurrentAuction();
                if (current == null) {
                    return "unknown";
                } else if (token.equalsIgnoreCase("itemamount")) {
                    return Integer.toString(current.getReward().getAmount());
                } else if (token.equalsIgnoreCase("time")) {
                    return TimeUtil.getFormattedTime(current.getTimeLeft(),
                            plugin.getConfig().getBoolean("general.shortenedTimeFormat", false));
                } else if (token.equalsIgnoreCase("autowin")) {
                    return plugin.formatDouble(current.getAutowin());
                } else if (token.equalsIgnoreCase("ownername")) {
                    return current.getOwnerName() == null ? "Console" : current.getOwnerName();
                } else if (token.equalsIgnoreCase("topbiddername")) {
                    return current.getTopBidderName() == null ? "Console" : current.getTopBidderName();
                } else if (token.equalsIgnoreCase("increment")) {
                    return plugin.formatDouble(current.getBidIncrement());
                } else if (token.equalsIgnoreCase("topbid")) {
                    return plugin.formatDouble(current.getTopBid());
                } else if (token.equalsIgnoreCase("taxpercent")) {
                    return plugin.formatDouble(current.getTax());
                } else if (token.equalsIgnoreCase("taxamount")) {
                    return plugin.formatDouble(current.getTaxAmount());
                } else if (token.equalsIgnoreCase("winnings")) {
                    return plugin.formatDouble(current.getTopBid() - current.getTaxAmount());
                } else if (token.equalsIgnoreCase("itemname")) {
                    return current.getReward().getName();
                } else if (token.equalsIgnoreCase("startprice")) {
                    return plugin.formatDouble(current.getStartPrice());
                } else {
                    return null;
                }
            }
        });
    }
}

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

package com.sainttx.auctions.listener;

import com.sainttx.auctions.AuctionPluginImpl;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.event.AuctionEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Listens for various auction events
 */
public class AuctionListener implements Listener {

    private AuctionPluginImpl plugin;

    public AuctionListener(final AuctionPluginImpl plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAuctionEnd(AuctionEndEvent event) {
        // Verify this feature is enabled
        if (!plugin.getConfig().getBoolean("auctionSettings.commandsAfterAuction.enable", false)) {
            return;
        }

        Auction auction = event.getAuction();

        // Check if there must be an auction winner
        if (plugin.getConfig().getBoolean("auctionSettings.commandsAfterAuction.onlyIfSold", true)
                && auction.getTopBidder() == null) {
            return;
        }

        // Verify we have a list of commands to go through
        if (!plugin.getConfig().isList("auctionSettings.commandsAfterAuction.commands")) {
            return;
        }

        // Execute commands
        List<String> commands = plugin.getConfig().getStringList("auctionSettings.commandsAfterAuction.commands");
        String winner = auction.getTopBidderName() == null ? "[winner]" : auction.getTopBidderName();
        for (String command : commands) {
            command = command.replace("[owner]", auction.getOwnerName());
            command = command.replace("[winner]", winner);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }
}

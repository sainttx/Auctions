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

package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Handles the /auction toggle command for the auction plugin
 */
public class ToggleCommand extends AuctionSubCommand {

    public ToggleCommand(AuctionPlugin plugin) {
        super(plugin, "auctions.command.toggle", "toggle", "t");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        plugin.getManager().setAuctioningDisabled(!plugin.getManager().isAuctioningDisabled());
        String message = plugin.getManager().isAuctioningDisabled() ? "messages.auctionsDisabled" : "messages.auctionsEnabled";
        plugin.getManager().getMessageHandler().broadcast(plugin.getMessage(message), false);
        return false;
    }
}

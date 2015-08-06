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

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction bid command for the auction plugin
 */
public class BidCommand extends AuctionSubCommand {

    public BidCommand() {
        super("auctions.command.bid", "bid", "b");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();
        Auction auction = manager.getCurrentAuction();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can place bids on auctions");
        } else if (args.length < 2 && !plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true)) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.bidSyntax"));
        } else if (auction == null) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (auction.getType() == AuctionType.SEALED && args.length < 2) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.bidSyntax"));
        } else {
            Player player = (Player) sender;

            double bid;

            try {
                bid = args.length < 2
                        ? (auction.hasBids() ? auction.getTopBid() + auction.getBidIncrement() : auction.getStartPrice())
                        : Double.parseDouble(args[1]);
            } catch (NumberFormatException ex) {
                handler.sendMessage(sender, plugin.getMessage("messages.error.invalidNumberEntered"));
                return true;
            }

            if (!player.hasPermission("auctions.bypass.general.disabledworld")
                    && plugin.isWorldDisabled(player.getWorld())) {
                handler.sendMessage(player, plugin.getMessage("messages.error.cantUsePluginInWorld"));
            } else if (handler.isIgnoring(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.currentlyIgnoring")); // player is ignoring
            } else if (auction.getOwner().equals(player.getUniqueId())) {
                handler.sendMessage(player, plugin.getMessage("messages.error.bidOnOwnAuction")); // cant bid on own auction
            } else {
                auction.placeBid(player, bid);
            }
        }

        return true;
    }
}

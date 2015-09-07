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
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.Auctions;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 09/05/2015.
 */
public class ImpoundCommand extends AuctionSubCommand {

    public ImpoundCommand(AuctionPlugin plugin) {
        super(plugin, "auctions.command.impound", "impound");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        MessageHandler handler = plugin.getManager().getMessageHandler();
        Auction auction = plugin.getManager().getCurrentAuction();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can impound auctions");
        } else if (auction == null) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else {
            Player player = (Player) sender;
            Reward reward = auction.getReward();
            auction.impound();
            reward.giveItem(player);

            String message = plugin.getMessage("messages.auctionImpounded")
                    .replace("[player]", player.getName());
            handler.broadcast(message, false);
        }

        return true;
    }
}

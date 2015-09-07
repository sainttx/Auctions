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
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Queue;

/**
 * Created by Matthew on 10/05/2015.
 */
public class QueueCommand extends AuctionSubCommand {

    public QueueCommand(AuctionPlugin plugin) {
        super(plugin, "auctions.command.queue", "queue", "q");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = plugin.getManager();
        Queue<Auction> queue = manager.getQueue();

        if (queue.isEmpty()) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noAuctionsInQueue"));
        } else {
            int queuePosition = 1;

            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.queueInfoHeader"));
            for (Auction auction : queue) {
                String message = plugin.getMessage("messages.auctionFormattable.queueInfoLine")
                        .replace("[queuepos]", Integer.toString(queuePosition));
                manager.getMessageHandler().sendMessage(sender, message, auction);
                queuePosition++;
            }
        }

        return true;
    }
}

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

import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction ignore command for the auction plugin
 */
public class IgnoreCommand extends AuctionSubCommand {

    public IgnoreCommand() {
        super("auctions.command.ignore", "ignore");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can ignore the plugin");
        } else {
            AuctionManager manager = AuctionsAPI.getAuctionManager();
            MessageHandler handler = manager.getMessageHandler();
            Player player = (Player) sender;

            if (manager.getMessageHandler().isIgnoring(player)) {
                handler.removeIgnoring(player);
                handler.sendMessage(player, plugin.getMessage("messages.noLongerIgnoring"));
            } else {
                handler.addIgnoring(player);
                handler.sendMessage(player, plugin.getMessage("messages.nowIgnoring"));
            }
        }
        return false;
    }
}

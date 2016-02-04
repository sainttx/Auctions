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
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction cancel command for the auction plugin
 */
public class CancelCommand extends AuctionSubCommand {

    public CancelCommand(AuctionPlugin plugin) {
        super(plugin, "auctions.command.cancel", "cancel", "c", "can");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = plugin.getManager();
        MessageHandler handler = manager.getMessageHandler();

        if (manager.getCurrentAuction() == null) {
            // No auction
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (sender instanceof Player && manager.getMessageHandler().isIgnoring(sender)) {
            // Ignoring
            handler.sendMessage(sender, plugin.getMessage("messages.error.currentlyIgnoring"));
        } else if (manager.getCurrentAuction().getTimeLeft() < plugin.getConfig().getInt("auctionSettings.mustCancelBefore", 15)
                && !sender.hasPermission("auctions.bypass.cancel.timer")) {
            // Can't cancel
            handler.sendMessage(sender, plugin.getMessage("messages.error.cantCancelNow"));
        } else if (plugin.getConfig().getInt("auctionSettings.mustCancelAfter", -1) != -1
                && manager.getCurrentAuction().getTimeLeft() > plugin.getConfig().getInt("auctionSettings.mustCancelAfter", -1)
                && !sender.hasPermission("auctions.bypass.cancel.timer")) {
            // Can't cancel
            handler.sendMessage(sender, plugin.getMessage("messages.error.cantCancelNow"));
        } else if (sender instanceof Player
                && !manager.getCurrentAuction().getOwner().equals(((Player) sender).getUniqueId())
                && !sender.hasPermission("auctions.bypass.cancel.otherauctions")) {
            // Can't cancel other peoples auction
            handler.sendMessage(sender, plugin.getMessage("messages.error.notYourAuction"));
        } else {
            manager.getCurrentAuction().cancel();
        }
        return false;
    }
}

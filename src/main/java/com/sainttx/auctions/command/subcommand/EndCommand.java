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
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction end command for the auction plugin
 */
public class EndCommand extends AuctionSubCommand {

    public EndCommand(AuctionPlugin plugin) {
        super(plugin, "auctions.command.end", "end", "e");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = plugin.getManager();

        if (manager.getCurrentAuction() == null) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (!sender.hasPermission("auctions.bypass.end.otherauctions")
                && sender instanceof Player
                && !manager.getCurrentAuction().getOwner().equals(((Player) sender).getUniqueId())) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.notYourAuction"));
        } else {
            manager.getCurrentAuction().end(true);
            manager.setCurrentAuction(null);
        }
        return false;
    }
}

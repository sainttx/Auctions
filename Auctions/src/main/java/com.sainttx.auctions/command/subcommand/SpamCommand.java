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

import com.sainttx.auctions.AuctionPluginImpl;
import com.sainttx.auctions.api.messages.MessageHandlerAddon.SpammyMessagePreventer;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to toggle spammy messages
 */
public class SpamCommand extends AuctionSubCommand {

    public SpamCommand(AuctionPluginImpl plugin) {
        super(plugin, "auctions.command.spam", "spam", "spammy", "hidespam", "togglespam");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle spammy auction messages");
        } else if (!(plugin.getMessageHandler() instanceof SpammyMessagePreventer)) {
            plugin.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.cantHideSpam"));
        } else {
            SpammyMessagePreventer preventer = (SpammyMessagePreventer) plugin.getMessageHandler();
            Player player = (Player) sender;

            if (!preventer.isIgnoringSpam(player.getUniqueId())) {
                preventer.addIgnoringSpam(player.getUniqueId());
                plugin.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.nowHidingSpam"));
            } else {
                preventer.removeIgnoringSpam(player.getUniqueId());
                plugin.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.noLongerHidingSpam"));
            }
        }
        return false;
    }
}

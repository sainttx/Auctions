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

import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.messages.MessageHandlerAddon.SpammyMessagePreventer;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to toggle spammy messages
 */
public class SpamCommand extends AuctionSubCommand {

    public SpamCommand() {
        super("auctions.command.spam", "spam", "spammy", "hidespam", "togglespam");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        MessageHandler handler = AuctionsAPI.getMessageHandler();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can toggle spammy auction messages");
        } else if (!(handler instanceof SpammyMessagePreventer)) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.cantHideSpam"));
        } else {
            SpammyMessagePreventer preventer = (SpammyMessagePreventer) AuctionsAPI.getMessageHandler();
            Player player = (Player) sender;

            if (!preventer.isIgnoringSpammy(player.getUniqueId())) {
                preventer.addIgnoringSpammy(player.getUniqueId());
                handler.sendMessage(sender, plugin.getMessage("messages.nowHidingSpam"));
            } else {
                preventer.removeIgnoringSpammy(player.getUniqueId());
                handler.sendMessage(sender, plugin.getMessage("messages.noLongerHidingSpam"));
            }
        }
        return false;
    }
}

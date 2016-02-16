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

package com.sainttx.auctions.api.messages;

import org.bukkit.command.CommandSender;

import java.util.Collection;

/**
 * Represents a group of players that can receive messages from the plugin
 */
public interface MessageRecipientGroup {

    /**
     * Returns all recipients inside the recipient group
     *
     * @return any players that are in the valid channel to receive the message
     */
    Collection<? extends CommandSender> getRecipients();
}

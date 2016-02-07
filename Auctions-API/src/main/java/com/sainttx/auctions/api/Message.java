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

package com.sainttx.auctions.api;

/**
 * Represents a configuration message that can be sent to a player.
 * {@link #getPath()} will return the corresponding path inside of
 * config.yml to the actual unformatted message.
 */
public interface Message {

    /**
     * Returns the path inside the configuration configuration path of the message.
     *
     * @return the path.
     */
    String getPath();

    /**
     * Returns whether or not this message is considered "spammy". Spammy messages
     * are able to be ignored by players using the /auction spam command.
     *
     * @return whether this message is considered spammy or not.
     */
    boolean isSpammy();

    /**
     * Returns whether or not this message can be ignored
     *
     * @return whether this message is ignorable or not.
     */
    boolean isIgnorable();
}

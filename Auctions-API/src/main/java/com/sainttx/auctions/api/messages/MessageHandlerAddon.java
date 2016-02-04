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

import java.util.UUID;

/**
 * Represents an option that can be attached to a message handler
 */
public interface MessageHandlerAddon {

    /**
     * Represents an option to prevent bid spam
     */
    interface SpammyMessagePreventer extends MessageHandlerAddon {

        /**
         * Sets a player to be ignoring all spammy messages
         *
         * @param uuid the players {@link UUID}
         */
        void addIgnoringSpam(UUID uuid);

        /**
         * Removes a player from ignoring all spammy messages
         *
         * @param uuid the players {@link UUID}
         */
        void removeIgnoringSpam(UUID uuid);

        /**
         * Gets whether a player is ignoring spammy messages
         *
         * @param uuid the players {@link UUID}
         * @return true if the player is ignoring bids
         */
        boolean isIgnoringSpam(UUID uuid);
    }
}

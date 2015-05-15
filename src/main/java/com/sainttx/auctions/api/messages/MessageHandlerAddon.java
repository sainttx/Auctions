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
        void addIgnoringSpammy(UUID uuid);

        /**
         * Removes a player from ignoring all spammy messages
         *
         * @param uuid the players {@link UUID}
         */
        void removeIgnoringSpammy(UUID uuid);

        /**
         * Gets whether a player is ignoring spammy messages
         *
         * @param uuid the players {@link UUID}
         * @return true if the player is ignoring bids
         */
        boolean isIgnoringSpammy(UUID uuid);
    }
}

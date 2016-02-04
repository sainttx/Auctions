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

import com.sainttx.auctions.api.Auction;
import org.bukkit.command.CommandSender;

import java.util.UUID;

/**
 * Represents a message handler
 */
public interface MessageHandler {

    /**
     * Broadcasts a message to all recipients that are listening
     *
     * @param message the message to send
     * @param force bypass auction ignore status
     */
    void broadcast(String message, boolean force);

    /**
     * Broadcasts a message to all recipients that are listening
     *
     * @param message the message to send
     * @param auction the auction to format the message with
     * @param force bypass auction ignore status
     */
    void broadcast(String message, Auction auction, boolean force);

    /**
     * Sends a message to a recipient
     *
     * @param recipient the receiver of the message
     * @param message the message to send
     */
    void sendMessage(CommandSender recipient, String message);

    /**
     * Sends a message to a recipient
     *
     * @param recipient the receiver of the message
     * @param message the message to send
     * @param auction the auction to format the message with
     */
    void sendMessage(CommandSender recipient, String message, Auction auction);

    /**
     * Sends an auctions information to a recipient
     *
     * @param recipient the recipient of the message
     * @param auction the auction to give information about
     */
    void sendAuctionInformation(CommandSender recipient, Auction auction);

    /**
     * Returns whether a commandsender is ignoring auctions
     *
     * @param sender the sender
     * @return true if the sender is ignoring auctions or if
     * the sender is not in the recipients list for broadcasts
     */
    boolean isIgnoring(CommandSender sender);

    /**
     * Sets a player to be ignoring all non-forced auction messages.
     *
     * @param sender the player
     */
    void addIgnoring(CommandSender sender);

    /**
     * Removes a player that's ignoring all auction messages
     *
     * @param sender the {@link UUID} of the player
     * @return true if a player was actually removed
     */
    boolean removeIgnoring(CommandSender sender);

    /**
     * Represents a message formatting class
     */
    interface MessageFormatter {

        /**
         * Formats a messages chat colors
         *
         * @param message the message to format
         * @return the formatted message
         */
        String format(String message);

        /**
         * Formats a message and replaces placeholders with an auctions information
         *
         * @param message the message to format
         * @param auction the auction
         * @return the formatted message
         */
        String format(String message, Auction auction);
    }
}

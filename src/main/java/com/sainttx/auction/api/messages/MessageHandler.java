package com.sainttx.auction.api.messages;

import com.sainttx.auction.api.Auction;
import org.bukkit.command.CommandSender;

import java.util.UUID;

/**
 * Represents a message handler
 */
public interface MessageHandler {

    /**
     * Broadcasts a message to all recipients in {@link #getRecipients()}
     *
     * @param message the message to send
     * @param force   bypass auction ignore status
     */
    void broadcast(String message, boolean force);

    /**
     * Broadcasts a message to all recipients in {@link #getRecipients()}
     *
     * @param auction the auction to format the message with
     * @param message the message to send
     * @param force   bypass auction ignore status
     */
    void broadcast(Auction auction, String message, boolean force);

    /**
     * Sends a message to a recipient
     *
     * @param message   the message to send
     * @param recipient the receiver of the message
     */
    void sendMessage(String message, CommandSender recipient);

    /**
     * Sends a message to a recipient
     *
     * @param auction   the auction to format the message with
     * @param message   the message to send
     * @param recipient the receiver of the message
     */
    void sendMessage(Auction auction, String message, CommandSender recipient);

    /**
     * Sends an auctions information to a recipient
     *
     * @param recipient the recipient of the message
     * @param auction   the auction to give information about
     */
    void sendAuctionInformation(CommandSender recipient, Auction auction);

    /**
     * Returns whether a player is ignoring auctions
     *
     * @param playerId the {@link UUID} of the player
     * @return true if the player is ignoring auctions
     */
    boolean isIgnoring(UUID playerId);

    /**
     * Sets a player to be ignoring all auction messages. All
     * added ignores carry over through any MessageHandlers that
     * extend the {@link AbstractMessageHandler} class
     *
     * @param playerId the {@link UUID} of the player
     */
    void addIgnoring(UUID playerId);

    /**
     * Removes a player that's ignoring all auction messages
     *
     * @param playerId the {@link UUID} of the player
     * @return true if a player was actually removed
     */
    boolean removeIgnoring(UUID playerId);

    /**
     * Gets the message formatter
     *
     * @return the message formatter
     */
    MessageFormatter getFormatter();

    /**
     * Returns all recipients inside the channel
     *
     * @return any players that are in the valid channel to receive the message
     */
    Iterable<? extends CommandSender> getRecipients();

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

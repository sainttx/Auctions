package com.sainttx.auction.api;

import com.sainttx.auction.api.messages.MessageHandler;
import org.bukkit.entity.Player;

import java.util.Queue;

/**
 * Handles all Auction management for the plugin
 */
public interface AuctionManager {

    /**
     * Gets the current active auction
     *
     * @return the current auction
     */
    Auction getCurrentAuction();

    /**
     * Gets the auction queue
     *
     * @return the auction queue
     */
    Queue<Auction> getQueue();

    /**
     * Returns the current message handler
     *
     * @return the message handler for auction messages
     */
    MessageHandler getMessageHandler();

    /**
     * Sets the plugins message handler
     *
     * @param handler the new message handler
     */
    void setMessageHandler(MessageHandler handler);

    /**
     * Adds an auction to the queue
     *
     * @param auction the auction to add
     */
    void addAuctionToQueue(Auction auction);

    /**
     * Gets the auction queue position of a player. Returns -1 if
     * the player has no auction in the queue.
     *
     * @param player the player
     * @return the queue position of a player
     */
    int getQueuePosition(Player player);

    /**
     * Gets whether a player has an auction in the queue or not
     *
     * @param player the player
     * @return true if the player has an auction in the queue, false otherwise
     */
    boolean hasAuctionInQueue(Player player);

    /**
     * Gets whether or not {@link #getCurrentAuction()} is
     * owned by the player
     *
     * @param player the player
     * @return true if the active auction is owned by the player
     */
    boolean hasActiveAuction(Player player);

    /**
     * Gets whether this plugin is disabled
     *
     * @return the disabled status of the plugin
     */
    boolean isAuctioningDisabled();

    /**
     * Sets the disabled status of the plugin
     *
     * @param disabled the new disabled status
     */
    void setAuctioningDisabled(boolean disabled);

    /**
     * Starts the next auction in the queue
     */
    void startNextAuction();
}

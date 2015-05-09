package com.sainttx.auction.api.messages;

import org.bukkit.entity.Player;

/**
 * Created by Matthew on 08/05/2015.
 */
public interface MessageHandler {

    /**
     * Broadcasts a message to all recipients in the channel
     *
     * @param configurationPath the path to the message in messages.yml
     * @param force             bypass auction ignore status
     */
    void sendMessage(String configurationPath, boolean force);

    /**
     * Broadcasts a message to all recipients in the channel
     *
     * @param auction           the auction to format the message with
     * @param configurationPath the path to the message in messages.yml
     * @param force             bypass auction ignore status
     */
    void sendMessage(com.sainttx.auction.Auction auction, String configurationPath, boolean force);

    /**
     * Broadcasts a message to a recipient
     *
     * @param configurationPath the path to the message in messages.yml
     * @param player            the receiver of the message
     */
    void sendMessage(String configurationPath, Player player);

    /**
     * Broadcasts a message to a recipient
     *
     * @param auction           the auction to format the message with
     * @param configurationPath the path to the message in messages.yml
     * @param player            the receiver of the message
     */
    void sendMessage(com.sainttx.auction.Auction auction, String configurationPath, Player player);

    /**
     * Returns all recipients inside the channel
     *
     * @return any players that are in the valid channel to receive the message
     */
    Iterable<? extends Player> getRecipients();
}

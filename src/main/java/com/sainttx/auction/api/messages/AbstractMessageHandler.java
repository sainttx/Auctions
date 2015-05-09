package com.sainttx.auction.api.messages;

import com.sainttx.auction.AuctionBlah;
import com.sainttx.auction.util.TextUtil;
import mkremins.fanciful.FancyMessage;
import org.bukkit.entity.Player;

/**
 * A base message handler that handles message sending
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    /**
     * Broadcasts a message to all recipients in the channel
     *
     * @param configurationPath the path to the message in messages.yml
     * @param force             bypass auction ignore status
     */
    public void sendMessage(String configurationPath, boolean force) {
        sendMessage(null, configurationPath, force);
    }

    /**
     * Broadcasts a message to all recipients in the channel
     *
     * @param auction           the auction to format the message with
     * @param configurationPath the path to the message in messages.yml
     * @param force             bypass auction ignore status
     */
    public void sendMessage(AuctionBlah auction, String configurationPath, boolean force) {
        String message = TextUtil.replace(auction, TextUtil.getConfigMessage(configurationPath));
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            FancyMessage fancy = TextUtil.createMessage(auction, msg);

            for (Player player : getRecipients()) {
                if (force || !TextUtil.isIgnoring(player.getUniqueId())) {
                    fancy.send(player);
                }
            }
        }
    }

    /**
     * Broadcasts a message to a recipient
     *
     * @param configurationPath the path to the message in messages.yml
     * @param player            the receiver of the message
     */
    public void sendMessage(String configurationPath, Player player) {
        sendMessage(null, configurationPath, player);
    }

    /**
     * Broadcasts a message to a recipient
     *
     * @param auction           the auction to format the message with
     * @param configurationPath the path to the message in messages.yml
     * @param player            the receiver of the message
     */
    public void sendMessage(AuctionBlah auction, String configurationPath, Player player) {
        String message = TextUtil.replace(auction, TextUtil.getConfigMessage(configurationPath));
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            FancyMessage fancy = TextUtil.createMessage(auction, msg);
            fancy.send(player);
        }
    }

    /**
     * Returns all recipients inside the channel
     *
     * @return any players that are in the valid channel to recieve the message
     */
    public abstract Iterable<? extends Player> getRecipients();
}

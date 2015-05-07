package me.sainttx.auction.struct;

import me.sainttx.auction.Auction;
import me.sainttx.auction.util.TextUtil;
import mkremins.fanciful.FancyMessage;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 07/05/2015.
 */
public abstract class MessageHandler {

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
    public void sendMessage(Auction auction, String configurationPath, boolean force) {
        FancyMessage message = TextUtil.createMessage(auction, configurationPath);

        for (Player player : getRecipients()) {
            if (force || !TextUtil.isIgnoring(player.getUniqueId())) {
                message.send(player);
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
    public void sendMessage(Auction auction, String configurationPath, Player player) {
        FancyMessage message = TextUtil.createMessage(auction, configurationPath);
        message.send(player);
    }

    /**
     * Returns all recipients inside the channel
     *
     * @return any players that are in the valid channel to recieve the message
     */
    public abstract Iterable<? extends Player> getRecipients();
}

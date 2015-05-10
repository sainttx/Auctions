package com.sainttx.auction.api.messages;

import org.bukkit.command.CommandSender;

/**
 * Represents a group of players that can receive messages from the plugin
 */
public interface MessageRecipientGroup {

    /**
     * Returns all recipients inside the recipient group
     *
     * @return any players that are in the valid channel to receive the message
     */
    Iterable<? extends CommandSender> getRecipients();
}

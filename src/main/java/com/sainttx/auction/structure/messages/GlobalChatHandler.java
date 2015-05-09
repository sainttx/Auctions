package com.sainttx.auction.structure.messages;

import com.sainttx.auction.api.messages.AbstractMessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Sends messages to all players
 */
public class GlobalChatHandler extends AbstractMessageHandler {

    @Override
    public Iterable<? extends Player> getRecipients() {
        return Bukkit.getOnlinePlayers();
    }
}

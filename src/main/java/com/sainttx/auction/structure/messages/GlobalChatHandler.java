package com.sainttx.auction.structure.messages;

import com.sainttx.auction.api.messages.AbstractMessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 07/05/2015.
 */
public class GlobalChatHandler extends AbstractMessageHandler {

    @Override
    public Iterable<? extends Player> getRecipients() {
        return Bukkit.getOnlinePlayers();
    }
}

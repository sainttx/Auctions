package com.sainttx.auctions.structure.messages.group;

import com.sainttx.auctions.api.messages.MessageRecipientGroup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Sends messages to all players
 */
public class GlobalChatGroup implements MessageRecipientGroup {

    @Override
    public Iterable<? extends CommandSender> getRecipients() {
        return Bukkit.getOnlinePlayers();
    }
}

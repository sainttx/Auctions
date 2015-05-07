package me.sainttx.auction.struct.messages;

import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.struct.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 07/05/2015.
 */
public class GlobalChatHandler extends MessageHandler {

    private AuctionPlugin plugin;

    public GlobalChatHandler(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Iterable<? extends Player> getRecipients() {
        return Bukkit.getOnlinePlayers();
    }
}

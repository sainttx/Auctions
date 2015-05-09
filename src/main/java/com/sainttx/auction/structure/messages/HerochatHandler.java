package com.sainttx.auction.structure.messages;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.messages.AbstractMessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Sends messages to all players inside a specific Herochat channel
 */
public class HerochatHandler extends AbstractMessageHandler {

    private AuctionPlugin plugin;

    public HerochatHandler(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Iterable<? extends Player> getRecipients() {
        return !isHerochatEnabled() ? new HashSet<Player>()
                : getChannelPlayers(plugin.getConfig().getString("integration.herochat.channel"));
    }

    /**
     * Returns whether or not Herochat is enabled
     *
     * @return true if the plugin is enabled
     */
    public boolean isHerochatEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Herochat");
    }

    /**
     * Returns whether or not a channel exists
     *
     * @param channel the name of the channel
     * @return true if the channel exists
     */
    public boolean isValidChannel(String channel) {
        Channel ch = Herochat.getChannelManager().getChannel(channel);
        return ch != null;
    }

    /**
     * Returns all players currently in a Herochat channel
     *
     * @param channel the name of the channel
     * @return all participants of the channel
     */
    public Set<Player> getChannelPlayers(String channel) {
        Set<Player> players = new HashSet<Player>();

        if (!isValidChannel(channel)) {
            plugin.getLogger().info("\"" + channel + "\" is not a valid channel, sending message to nobody.");
            return players;
        }

        Channel ch = Herochat.getChannelManager().getChannel(channel);
        Set<Chatter> members = ch.getMembers();

        for (Chatter c : members) {
            players.add(c.getPlayer());
        }

        return players;
    }
}

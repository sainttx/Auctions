/*
 * Copyright (C) SainttX <http://sainttx.com>
 * Copyright (C) contributors
 *
 * This file is part of Auctions.
 *
 * Auctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Auctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Auctions.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sainttx.auctions.structure.messages.group;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import com.sainttx.auctions.AuctionPluginImpl;
import com.sainttx.auctions.api.messages.MessageRecipientGroup;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sends messages to all players inside a specific Herochat channel
 */
public class HerochatGroup implements MessageRecipientGroup {

    private AuctionPluginImpl plugin;

    public HerochatGroup(AuctionPluginImpl plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<? extends CommandSender> getRecipients() {
        return Collections.emptyList();
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
        Set<Player> players = new HashSet<>();

        if (!isValidChannel(channel)) {
            plugin.getLogger().info("\"" + channel + "\" is not a valid channel, sending message to nobody.");
            return players;
        }

        Channel ch = Herochat.getChannelManager().getChannel(channel);
        Set<Chatter> members = ch.getMembers();

        players.addAll(
                members.stream().map(Chatter::getPlayer).collect(Collectors.toList())
        );

        return players;
    }
}

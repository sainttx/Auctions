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

package com.sainttx.auctions.recipient;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import com.sainttx.auctions.api.messages.MessageGroup;
import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Sends messages to all players inside a specific Herochat channel
 */
public class HerochatChannelGroup implements MessageGroup {

    private Channel channel;

    public HerochatChannelGroup(String channel) {
        this(Herochat.getChannelManager().getChannel(channel));
    }

    public HerochatChannelGroup(Channel channel) {
        Validate.notNull(channel, "Channel cannot be null");
        this.channel = channel;
    }

    @Override
    public Collection<? extends CommandSender> getRecipients() {
        return channel.getMembers().stream().map(Chatter::getPlayer).collect(Collectors.toList());
    }
}

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

package com.sainttx.auctions.structure.messages.handler;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObject;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObjectv1_8_R1;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObjectv1_8_R3;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObjectv1_9_R1;
import com.sainttx.auctions.util.ReflectionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 10/05/2015.
 */
public class ActionBarMessageHandler extends TextualMessageHandler {

    private ActionBarObject base;

    public ActionBarMessageHandler(AuctionPlugin plugin) {
        super(plugin);

        String version = ReflectionUtil.getVersion();
        if (version.startsWith("v1_8_R1")) {
            this.base = new ActionBarObjectv1_8_R1(plugin);
        } else if (version.startsWith("v1_8_R2") || version.startsWith("v1_8_R3")) {
            this.base = new ActionBarObjectv1_8_R3(plugin);
        } else if (version.startsWith("v1_9_R1")) {
            this.base = new ActionBarObjectv1_9_R1(plugin);
        } else {
            throw new IllegalStateException("this server version is unsupported");
        }
    }

    @Override
    public void broadcast(String message, Auction auction, boolean spammy) {
        super.broadcast(message, auction, spammy);
        message = formatter.format(this.plugin.getMessage("messages.auctionFormattable.actionBarMessage"), auction);

        if (!message.isEmpty()) {
            base.setTitle(message);

            for (CommandSender recipient : getAllRecipients()) {
                if (recipient instanceof Player && !isIgnoring(recipient)) {
                    base.send((Player) recipient);
                }
            }
        }
    }
}

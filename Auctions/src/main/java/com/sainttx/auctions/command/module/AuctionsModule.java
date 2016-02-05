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

package com.sainttx.auctions.command.module;

import com.sainttx.auctions.api.Auction;
import com.sk89q.intake.parametric.AbstractModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuctionsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CommandSender.class).toProvider(new CommandSenderProvider());
        bind(Player.class).toProvider(new PlayerProvider());
        bind(Auction.class).toProvider(new CurrentAuctionProvider());
    }
}

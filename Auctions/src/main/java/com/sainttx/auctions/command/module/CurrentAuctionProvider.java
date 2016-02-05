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
import com.sainttx.auctions.api.AuctionPlugin;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.MissingArgumentException;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;

import java.lang.annotation.Annotation;
import java.util.List;

public class CurrentAuctionProvider implements Provider<Auction> {

    @Override
    public boolean isProvided() {
        return false;
    }

    @Override
    public Auction get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
        AuctionPlugin plugin = arguments.getNamespace().get(AuctionPlugin.class);
        if (plugin == null) {
            throw new MissingArgumentException();
        } else {
            return plugin.getManager().getCurrentAuction();
        }
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return null;
    }
}

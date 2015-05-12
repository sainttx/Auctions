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

package com.sainttx.auctions.api.module;

import com.sainttx.auctions.api.Auction;

/**
 * Represents a module that can be added onto an {@link Auction}.
 * <p>
 * Added Auction module implementations are always triggered after a
 * new bid is successfully placed on an auction. An example of an
 * implementation is an AntiSnipe Module which checks if an anti snipe
 * instance can be triggered on an Auction and then adds time if it triggers.
 * </p>
 */
public interface AuctionModule {

    /**
     * Gets whether or not the module can be triggered
     *
     * @return true if the module can be triggered
     */
    boolean canTrigger();

    /**
     * Triggers the action in this module
     */
    void trigger();
}

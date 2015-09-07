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

package com.sainttx.auctions.api.event;

import com.sainttx.auctions.api.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Created by Matthew on 9/7/2015.
 */
public class AuctionPreBidEvent extends AuctionEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player who;
    private final double amount;
    private boolean cancelled;

    /**
     * Called when a player attempts to bid on an auction with a command
     *
     * @param auction the affected auction
     * @param who     the bidding player
     * @param amount  the amount that the player is bidding
     */
    public AuctionPreBidEvent(final Auction auction, Player who, double amount) {
        super(auction);
        this.who = who;
        this.amount = amount;
    }

    /**
     * Gets the bidding player
     *
     * @return the bidding player
     */
    public Player getPlayer() {
        return who;
    }

    /**
     * Gets the amount to bid
     *
     * @return the amount to bid
     */
    public double getAmount() {
        return amount;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
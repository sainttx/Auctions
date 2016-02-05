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

package com.sainttx.auctions.structure;

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.event.AuctionStartEvent;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.module.AuctionModule;
import com.sainttx.auctions.api.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * An auction implementation
 */
public abstract class AbstractAuction implements Auction {

    // Instance
    protected AuctionPlugin plugin;
    protected Collection<AuctionModule> modules = new HashSet<>();

    // Auction owner information
    protected UUID ownerUUID;
    protected String ownerName;

    // Top bidder information
    protected UUID topBidderUUID;
    protected String topBidderName;
    protected double winningBid;
    protected double startPrice;
    protected boolean hasBidBeenPlaced;

    // Auction information
    protected Reward reward;
    protected double bidIncrement;
    protected double autowin = -1;
    protected int timeLeft;
    protected BukkitTask timerTask;

    @Override
    public UUID getOwner() {
        return ownerUUID;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public boolean hasBids() {
        return hasBidBeenPlaced;
    }

    @Override
    public boolean hasEnded() {
        return timeLeft <= 0 || (getAutowin() != -1 && getAutowin() < getTopBid());
    }

    @Override
    public UUID getTopBidder() {
        return topBidderUUID;
    }

    @Override
    public String getTopBidderName() {
        return topBidderName;
    }

    @Override
    public Reward getReward() {
        return reward;
    }

    @Override
    public double getTopBid() {
        return winningBid;
    }

    @Override
    public double getStartPrice() {
        return startPrice;
    }

    @Override
    public double getAutowin() {
        return autowin;
    }

    @Override
    public int getTimeLeft() {
        return timeLeft;
    }

    @Override
    public void setTimeLeft(int time) {
        this.timeLeft = time;
    }

    @Override
    public void start() {
        this.timerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new AuctionTimer(), 20L, 20L);
        startMessages();
        AuctionStartEvent event = new AuctionStartEvent(this);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public double getBidIncrement() {
        return bidIncrement;
    }

    @Override
    public double getTax() {
        return plugin.getConfig().getInt("auctionSettings.taxPercent", 0);
    }

    @Override
    public double getTaxAmount() {
        return (getTopBid() * getTax()) / 100;
    }

    @Override
    public Collection<AuctionModule> getModules() {
        return new HashSet<>(modules);
    }

    @Override
    public void addModule(AuctionModule module) {
        if (module == null) {
            throw new IllegalArgumentException("module cannot be null");
        }

        this.modules.add(module);
    }

    @Override
    public boolean removeModule(AuctionModule module) {
        return this.modules.remove(module);
    }

    @Override
    public abstract void cancel();

    @Override
    public abstract void end(boolean broadcast);

    @Override
    public abstract void impound();

    @Override
    public abstract void placeBid(Player player, double bid);

    /**
     * Schedules a new auction after a 'auctionSettings.delayBetween' second delay
     */
    public abstract void runNextAuctionTimer();

    /**
     * Dispatches messages for the start of the auction
     */
    protected abstract void startMessages();

    /**
     * Returns all bidders money
     */
    public abstract void returnMoneyToAll();

    /**
     * Broadcasts the most recent bid
     */
    public abstract void broadcastBid();

    /**
     * An implementation of an auction timer
     */
    public class AuctionTimer implements Auction.Timer {

        @Override
        public void run() {
            timeLeft--;

            if (timeLeft <= 0) {
                end(true);
            } else if (plugin.isBroadcastTime(timeLeft)) {
                MessageHandler handler = plugin.getManager().getMessageHandler();
                handler.broadcast(plugin.getMessage("messages.auctionFormattable.timer"), AbstractAuction.this,
                        true);
            }
        }
    }
}

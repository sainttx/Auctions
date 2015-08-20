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

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.module.AuctionModule;
import com.sainttx.auctions.api.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created by Matthew on 11/05/2015.
 */
public class DefaultAuction extends AbstractAuction {

    protected DefaultAuction(AuctionPlugin plugin, AuctionType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public void placeBid(Player player, double bid) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        MessageHandler handler = AuctionsAPI.getMessageHandler();

        if (bid < (hasBids() ? getTopBid() + getBidIncrement() : getStartPrice())) {
            handler.sendMessage(player, plugin.getMessage("messages.error.bidTooLow")); // the bid wasnt enough
        } else if (plugin.getEconomy().getBalance(player) < bid) {
            handler.sendMessage(player, plugin.getMessage("messages.error.insufficientBalance")); // insufficient funds
        } else if (player.getUniqueId().equals(getTopBidder())) {
            handler.sendMessage(player, plugin.getMessage("messages.error.alreadyTopBidder")); // already top bidder
        } else {
            if (getTopBidder() != null) { // give the old winner their money back
                OfflinePlayer oldPlayer = Bukkit.getOfflinePlayer(getTopBidder());
                plugin.getEconomy().depositPlayer(oldPlayer, getTopBid());
            }

            this.hasBidBeenPlaced = true;
            this.winningBid = bid;
            this.topBidderName = player.getName();
            this.topBidderUUID = player.getUniqueId();
            plugin.getEconomy().withdrawPlayer(player, bid);
            broadcastBid();

            // Tell the player a personal bid message
            String message = plugin.getMessage("messages.bid")
                    .replace("[bid]", plugin.formatDouble(bid));
            handler.sendMessage(player, message);

            // Trigger our modules
            for (AuctionModule module : modules) {
                if (module.canTrigger()) {
                    module.trigger();
                }
            }
        }
    }

    @Override
    protected void startMessages() {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();

        handler.broadcast(plugin.getMessage("messages.auctionFormattable.start"), this, false);
        handler.broadcast(plugin.getMessage("messages.auctionFormattable.price"), this, false);
        handler.broadcast(plugin.getMessage("messages.auctionFormattable.increment"), this, false);

        if (getAutowin() != -1) {
            handler.broadcast(plugin.getMessage("messages.auctionFormattable.autowin"), this, false);
        }
    }

    @Override
    public void impound() {
        timerTask.cancel();
        timerTask = null;
        runNextAuctionTimer();

        // Return the top bidders money
        returnMoneyToAll();

        // Set current auction to null
        AuctionsAPI.getAuctionManager().setCurrentAuction(null);
    }

    @Override
    public void cancel() {
        Player owner = Bukkit.getPlayer(ownerUUID);
        timerTask.cancel();
        timerTask = null;

        // Run the next auction timer
        if (plugin.isEnabled()) {
            runNextAuctionTimer(); // This handles setting the canStartNewAuction status
        }

        // Return the top bidders money
        returnMoneyToAll();

        // Broadcast
        MessageHandler handler = AuctionsAPI.getAuctionManager().getMessageHandler();
        handler.broadcast(plugin.getMessage("messages.auctionFormattable.cancelled"), this, false);

        // Return the item to the owner
        if (owner == null) {
            plugin.getLogger().info("Saving items of offline player " + getOwnerName() + " (uuid: " + getOwner() + ")");
            plugin.saveOfflinePlayer(getOwner(), getReward());
        } else {
            getReward().giveItem(owner);
            handler.sendMessage(owner, plugin.getMessage("messages.ownerItemReturn"));
        }

        // Set current auction to null
        AuctionsAPI.getAuctionManager().setCurrentAuction(null);
    }

    @Override
    public void end(boolean broadcast) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();
        Player owner = Bukkit.getPlayer(getOwner());

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        // Run the next auction timer
        if (plugin.isEnabled()) {
            runNextAuctionTimer();
        }

        if (getTopBidder() != null) {
            Player winner = Bukkit.getPlayer(getTopBidder());

            if (broadcast && (autowin == -1 || getTopBid() < getAutowin())) {
                handler.broadcast(plugin.getMessage("messages.auctionFormattable.end"), this, false);
            }

            if (getTopBid() > 0) {
                double winnings = getTopBid() - getTaxAmount();
                plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(getOwner()), winnings);

                if (owner != null) {
                    if (getTax() > 0) {
                        handler.sendMessage(owner, plugin.getMessage("messages.auctionFormattable.endTax"), this);
                    }
                    handler.sendMessage(owner, plugin.getMessage("messages.auctionFormattable.endNotifyOwner"), this);
                }
            }

            // Give the winner their items
            if (winner == null) {
                plugin.getLogger().info("Saving items of offline player " + getTopBidderName() + " (uuid: " + getTopBidder() + ")");
                plugin.saveOfflinePlayer(getTopBidder(), getReward());
            } else {
                getReward().giveItem(winner);
                handler.sendMessage(winner, plugin.getMessage("messages.auctionFormattable.winner"), this);
            }
        } else {
            if (broadcast) {
                handler.broadcast(plugin.getMessage("messages.auctionFormattable.endNoBid"), this, false);
            }
            if (owner != null) {
                handler.sendMessage(owner, plugin.getMessage("messages.ownerItemReturn"));
                getReward().giveItem(owner);
            } else {
                plugin.getLogger().info("Saving items of offline player " + getOwnerName() + " (uuid: " + getOwner() + ")");
                plugin.saveOfflinePlayer(getOwner(), getReward());
            }
        }

        // Set current auction to null
        AuctionsAPI.getAuctionManager().setCurrentAuction(null);
    }

    @Override
    public void returnMoneyToAll() {
        if (getTopBidder() != null) {
            OfflinePlayer topBidder = Bukkit.getOfflinePlayer(getTopBidder());
            plugin.getEconomy().depositPlayer(topBidder, getTopBid());
        }
    }

    @Override
    public void broadcastBid() {
        AuctionsAPI.getMessageHandler().broadcast(plugin.getMessage("messages.auctionFormattable.bid"), this, true);
    }

    @Override
    public void runNextAuctionTimer() {
        // Delay before a new auction can be made... Prevents auction scamming
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    AuctionsAPI.getAuctionManager().setCanStartNewAuction(true);

                    // Start the next auction in the queue
                    if (AuctionsAPI.getAuctionManager().getCurrentAuction() == null) {
                        AuctionsAPI.getAuctionManager().startNextAuction();
                    }
                }
            }, plugin.getConfig().getLong("auctionSettings.delayBetween", 5L) * 20L);
        }
    }


    /**
     * An implementation of an Auction builder for auctions
     */
    public static abstract class DefaultAuctionBuilder implements Builder {

        protected AuctionPlugin plugin;
        protected double increment = -1;
        protected int time = -1;
        protected Reward reward;
        protected double bid = -1;
        protected double autowin = -1;
        protected UUID ownerId;
        protected String ownerName;

        public DefaultAuctionBuilder(AuctionPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public Builder bidIncrement(double increment) {
            this.increment = increment;
            return this;
        }

        @Override
        public Builder owner(Player owner) {
            this.ownerId = owner.getUniqueId();
            this.ownerName = owner.getName();
            return this;
        }

        @Override
        public Builder time(int time) {
            this.time = time;
            return this;
        }

        @Override
        public Builder reward(Reward reward) {
            this.reward = reward;
            return this;
        }

        @Override
        public Builder topBid(double bid) {
            this.bid = bid;
            return this;
        }

        @Override
        public Builder autowin(double autowin) {
            this.autowin = autowin;
            return this;
        }

        /**
         * Initializes any default values that haven't been set
         */
        protected void defaults() {
            if (reward == null) {
                throw new IllegalStateException("reward cannot be null");
            } else if (bid == -1) {
                throw new IllegalStateException("bid hasn't been set");
            }
            if (increment == -1) {
                increment = plugin.getConfig().getInt("auctionSettings.defaultBidIncrement", 50);
            }
            if (time == -1) {
                time = plugin.getConfig().getInt("auctionSettings.startTime", 30);
            }
        }
    }
}

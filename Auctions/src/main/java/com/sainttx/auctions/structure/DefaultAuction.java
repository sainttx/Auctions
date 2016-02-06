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

import com.sainttx.auctions.MessagePath;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.event.AuctionEndEvent;
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

    public DefaultAuction(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void placeBid(Player player, double bid) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        if (bid < (hasBids() ? getTopBid() + getBidIncrement() : getStartPrice())) {
            plugin.getMessageFactory().submit(player, MessagePath.ERROR_BID_LOW);
        } else if (plugin.getEconomy().getBalance(player) < bid) {
            plugin.getMessageFactory().submit(player, MessagePath.ERROR_MONEY);
        } else if (player.getUniqueId().equals(getTopBidder())) {
            plugin.getMessageFactory().submit(player, MessagePath.ERROR_TOP_BIDDER);
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
            // TODO: Replace [bid] with winningBid
            plugin.getMessageFactory().submit(player, MessagePath.AUCTION_BID);

            // Trigger our modules
            modules.stream().filter(AuctionModule::canTrigger).forEach(AuctionModule::trigger);
        }
    }

    @Override
    protected void startMessages() {
        // TODO: Message groups
        plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_START, this);
        plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_PRICE, this);
        plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_INCREMENT, this);
        if (getAutowin() != -1) {
            plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_AUTOWIN, this);
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
        plugin.getManager().setCurrentAuction(null);
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

        // TODO: Group message
        plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_CANCELLED, this);

        // Return the item to the owner
        if (owner == null) {
            plugin.getLogger().info("Saving items of offline player " + getOwnerName() + " (uuid: " + getOwner() + ")");
            plugin.saveOfflinePlayer(getOwner(), getReward());
        } else {
            getReward().giveItem(owner);
            plugin.getMessageFactory().submit(owner, MessagePath.GENERAL_ITEM_RETURN, this);
        }

        // Set current auction to null
        plugin.getManager().setCurrentAuction(null);
    }

    @Override
    public void end(boolean broadcast) {
        MessageHandler handler = plugin.getManager().getMessageHandler();
        Player owner = Bukkit.getPlayer(getOwner());

        AuctionEndEvent event = new AuctionEndEvent(this);
        Bukkit.getPluginManager().callEvent(event);

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
                // TODO: Message groups
                plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_END, this);
            }

            if (getTopBid() > 0) {
                double winnings = getTopBid() - getTaxAmount();
                plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(getOwner()), winnings);

                if (owner != null) {
                    if (plugin.getSettings().getTaxPercent() > 0) {
                        plugin.getMessageFactory().submit(owner, MessagePath.AUCTION_END_TAX, this);
                    }
                    plugin.getMessageFactory().submit(owner, MessagePath.AUCTION_END_OWNERMSG, this);
                }
            }

            // Give the winner their items
            if (winner == null) {
                plugin.getLogger().info("Saving items of offline player " + getTopBidderName() + " (uuid: " + getTopBidder() + ")");
                plugin.saveOfflinePlayer(getTopBidder(), getReward());
            } else {
                getReward().giveItem(winner);
                plugin.getMessageFactory().submit(winner, MessagePath.AUCTION_WINNER, this);
            }
        } else {
            if (broadcast) {
                // TODO: Group message
                plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_END_NOBID, this);
            }
            if (owner != null) {
                plugin.getMessageFactory().submit(owner, MessagePath.GENERAL_ITEM_RETURN);
                getReward().giveItem(owner);
            } else {
                plugin.getLogger().info("Saving items of offline player " + getOwnerName() + " (uuid: " + getOwner() + ")");
                plugin.saveOfflinePlayer(getOwner(), getReward());
            }
        }

        // Set current auction to null
        plugin.getManager().setCurrentAuction(null);
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
        // TODO: Group message
        plugin.getMessageFactory().submit(Bukkit.getOnlinePlayers(), MessagePath.AUCTION_BID, this);
    }

    @Override
    public void runNextAuctionTimer() {
        // Delay before a new auction can be made... Prevents auction scamming
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.getManager().setCanStartNewAuction(true);

                    // Start the next auction in the queue
                    if (plugin.getManager().getCurrentAuction() == null) {
                        plugin.getManager().startNextAuction();
                    }
                }
            }, plugin.getSettings().getDelayBetweenAuctions() * 20L);
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
                increment = plugin.getSettings().getDefaultBidIncrement();
            }
            if (time == -1) {
                time = plugin.getSettings().getStartTime();
            }
        }
    }
}

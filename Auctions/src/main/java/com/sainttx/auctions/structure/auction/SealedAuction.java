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

package com.sainttx.auctions.structure.auction;

import com.sainttx.auctions.AuctionPluginImpl;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.module.AuctionModule;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.structure.DefaultAuction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An auction that will not broadcast any bids
 */
public class SealedAuction extends DefaultAuction {

    private Map<UUID, Double> currentBids = new HashMap<>();
    private Map<UUID, Integer> amountOfBids = new HashMap<>();

    /**
     * Creates an Auction
     *
     * @param plugin the auction plugin instance
     */
    private SealedAuction(AuctionPlugin plugin, UUID ownerUUID, String ownerName,
                          double topBid, Reward reward, double autowin, double bidIncrement, int timeLeft) {
        super(plugin, AuctionType.SEALED);
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.winningBid = topBid;
        this.startPrice = topBid;
        this.reward = reward;
        this.autowin = autowin;
        this.bidIncrement = bidIncrement;
        this.timeLeft = timeLeft;
    }

    @Override
    public void placeBid(Player player, double bid) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        if (bid < this.startPrice) {
            plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.bidTooLow"));
        } else if (amountOfBids.containsKey(player.getUniqueId())
                && amountOfBids.get(player.getUniqueId()) >= plugin.getConfig().getInt("auctionSettings.sealedAuctions.maxBidsPerPlayer", 1)) {
            plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.sealedAuctionsMaxBidsReached"));
        } else {
            double raiseAmount = bid;
            double previousBid = currentBids.containsKey(player.getUniqueId())
                    ? currentBids.get(player.getUniqueId()) : 0;

            if (previousBid > 0) {
                if (bid < previousBid + getBidIncrement()) {
                    plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.bidTooLow"));
                    return;
                } else if (bid <= previousBid) {
                    plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.sealedAuctionHaveHigherBid"));
                    return;
                } else {
                    raiseAmount -= previousBid;
                }
            }

            if (plugin.getEconomy().getBalance(player) < raiseAmount) {
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.insufficientBalance")); // insufficient funds
            } else {
                currentBids.put(player.getUniqueId(), bid);
                amountOfBids.put(player.getUniqueId(), amountOfBids.containsKey(player.getUniqueId())
                        ? amountOfBids.get(player.getUniqueId()) + 1 : 1);

                if (bid > winningBid) {
                    this.winningBid = bid;
                    this.topBidderName = player.getName();
                    this.topBidderUUID = player.getUniqueId();
                }

                if (previousBid == 0) {
                    String message = plugin.getMessage("messages.auctionFormattable.sealedAuction.bid")
                            .replace("[bid]", plugin.formatDouble(bid));
                    plugin.getMessageHandler().sendMessage(player, message);
                } else {
                    String message = plugin.getMessage("messages.auctionFormattable.sealedAuction.raise")
                            .replace("[bid]", plugin.formatDouble(bid))
                            .replace("[previous]", plugin.formatDouble(previousBid));
                    plugin.getMessageHandler().sendMessage(player, message);
                }

                // Raise amount
                plugin.getEconomy().withdrawPlayer(player, raiseAmount);

                // Trigger modules
                modules.stream()
                        .filter(AuctionModule::canTrigger)
                        .forEach(AuctionModule::trigger);
            }
        }

        return;
    }

    @Override
    public void end(boolean broadcast) {
        super.end(broadcast);

        // Return all money except for top bidder
        if (getTopBidder() != null) {
            currentBids.entrySet().stream()
                    .filter(bidder -> !bidder.getKey().equals(getTopBidder()))
                    .forEach(bidder -> {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(bidder.getKey());
                        double bid = bidder.getValue();

                        plugin.getEconomy().depositPlayer(player, bid);
                    });
        }
    }

    @Override
    public void broadcastBid() {
        // Don't broadcast anything
    }

    @Override
    protected void startMessages() {
        super.startMessages();
        plugin.getMessageHandler().broadcast(plugin.getMessage("messages.notifySealedAuction"), this, false);
    }

    @Override
    public void returnMoneyToAll() {
        for (Map.Entry<UUID, Double> bidder : currentBids.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(bidder.getKey());
            double bid = bidder.getValue();

            plugin.getEconomy().depositPlayer(player, bid);
        }
    }

    /**
     * An implementation of an Auction builder for silent auctions
     */
    public static class SealedAuctionBuilder extends DefaultAuctionBuilder {

        public SealedAuctionBuilder(AuctionPluginImpl plugin) {
            super(plugin);
        }

        @Override
        public Auction build() {
            super.defaults();
            return new SealedAuction(this.plugin, this.ownerId, this.ownerName,
                    this.bid, this.reward, this.autowin, this.increment, this.time);
        }
    }
}

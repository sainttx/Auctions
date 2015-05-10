package com.sainttx.auction.structure;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionType;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.api.module.AuctionModule;
import com.sainttx.auction.api.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An auction that will not broadcast any bids
 */
public class SealedAuction extends AbstractAuction {

    protected double startBid;
    private Map<UUID, Double> currentBids = new HashMap<UUID, Double>();
    private Map<UUID, Integer> amountOfBids = new HashMap<UUID, Integer>();

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
        this.startBid = topBid;
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

        MessageHandler handler = AuctionsAPI.getMessageHandler();

        if (bid < this.startBid) {
            handler.sendMessage(player, plugin.getMessage("messages.error.bidTooLow"));
        } else if (amountOfBids.containsKey(player.getUniqueId())
                && amountOfBids.get(player.getUniqueId()) >= plugin.getConfig().getInt("auctionSettings.sealedAuctions.maxBidsPerPlayer", 1)) {
            handler.sendMessage(player, plugin.getMessage("messages.error.sealedAuctionsMaxBidsReached"));
        } else {
            double raiseAmount = bid;
            double previousBid = currentBids.containsKey(player.getUniqueId())
                    ? currentBids.get(player.getUniqueId()) : 0;

            if (previousBid > 0) {
                if (bid < previousBid + getBidIncrement()) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.bidTooLow"));
                    return;
                } else if (bid <= previousBid) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.sealedAuctionHaveHigherBid"));
                    return;
                } else {
                    raiseAmount -= previousBid;
                }
            }

            if (plugin.getEconomy().getBalance(player) < raiseAmount) {
                handler.sendMessage(player, plugin.getMessage("messages.error.insufficientBalance")); // insufficient funds
            } else {
                currentBids.put(player.getUniqueId(), bid);

                if (bid > winningBid) {
                    this.winningBid = bid;
                    this.topBidderName = player.getName();
                    this.topBidderUUID = player.getUniqueId();
                }

                if (previousBid == 0) {
                    String message = plugin.getMessage("messages.auctionFormattable.sealedAuction.bid")
                            .replace("[bid]", plugin.formatDouble(bid));
                    handler.sendMessage(player, message);
                } else {
                    String message = plugin.getMessage("messages.auctionFormattable.sealedAuction.raise")
                            .replace("[bid]", plugin.formatDouble(bid))
                            .replace("[previous]", plugin.formatDouble(previousBid));
                    handler.sendMessage(player, message);
                }

                // Raise amount
                plugin.getEconomy().withdrawPlayer(player, raiseAmount);

                // Trigger modules
                for (AuctionModule module : modules) {
                    if (module.canTrigger()) {
                        module.trigger();
                    }
                }
            }
        }

        return;
    }

    @Override
    public void end(boolean broadcast) {
        super.end(broadcast);

        // Return all money except for top bidder
        if (getTopBidder() != null) {
            for (Map.Entry<UUID, Double> bidder : currentBids.entrySet()) {
                if (!bidder.getKey().equals(getTopBidder())) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(bidder.getKey());
                    double bid = bidder.getValue();

                    plugin.getEconomy().depositPlayer(player, bid);
                }
            }
        }
    }

    @Override
    public void broadcastBid() {
    }

    @Override
    protected void startMessages() {
        super.startMessages();
        AuctionsAPI.getMessageHandler().broadcast(plugin.getMessage("messages.notifySealedAuction"), this, false);
    }

    @Override
    protected void returnMoneyToAll() {
        for (Map.Entry<UUID, Double> bidder : currentBids.entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(bidder.getKey());
            double bid = bidder.getValue();

            plugin.getEconomy().depositPlayer(player, bid);
        }
    }

    /**
     * An implementation of an Auction builder for silent auctions
     */
    public static class SealedAuctionBuilder extends AbstractAuctionBuilder {

        public SealedAuctionBuilder(AuctionPlugin plugin) {
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

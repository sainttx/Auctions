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

package com.sainttx.auctions.command;

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.event.AuctionCreateEvent;
import com.sainttx.auctions.api.event.AuctionPreBidEvent;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.messages.MessageHandlerAddon;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.structure.auction.StandardAuction;
import com.sainttx.auctions.structure.module.AntiSnipeModule;
import com.sainttx.auctions.structure.module.AutoWinModule;
import com.sainttx.auctions.util.AuctionUtil;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Queue;

public class AuctionCommands {

    private final AuctionPlugin plugin;
    private final AuctionManager manager;

    public AuctionCommands(final AuctionPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
    }

    @Command(
            aliases = "impound",
            desc = "Impound the currently active auction.",
            max = 0
    )
    @Require("auctions.command.impound")
    public void impound(CommandSender sender, @Optional Auction auction) {
        MessageHandler handler = plugin.getManager().getMessageHandler();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can impound auctions");
        } else if (auction == null) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else {
            Player player = (Player) sender;
            Reward reward = auction.getReward();
            auction.impound();
            reward.giveItem(player);

            String message = plugin.getMessage("messages.auctionImpounded")
                    .replace("[player]", player.getName());
            handler.broadcast(message, false);
        }
    }

    @Command(
            aliases = "bid",
            usage = "[amount]",
            desc = "Bid on the currently active auction.",
            max = 1
    )
    @Require("auctions.command.bid")
    public void bid(Player player, @Optional Auction auction, @Optional Double amount) {
        MessageHandler handler = plugin.getManager().getMessageHandler();

        if (amount == null && !plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true)) {
            handler.sendMessage(player, plugin.getMessage("messages.error.bidSyntax"));
        } else if (auction == null) {
            handler.sendMessage(player, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (auction.getType() == AuctionType.SEALED && amount == null) {
            handler.sendMessage(player, plugin.getMessage("messages.error.bidSyntax"));
        } else {
            if (!player.hasPermission("auctions.bypass.general.disabledworld")
                    && plugin.isWorldDisabled(player.getWorld())) {
                handler.sendMessage(player, plugin.getMessage("messages.error.cantUsePluginInWorld"));
            } else if (handler.isIgnoring(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.currentlyIgnoring")); // player is ignoring
            } else if (auction.getOwner().equals(player.getUniqueId())) {
                handler.sendMessage(player, plugin.getMessage("messages.error.bidOnOwnAuction")); // cant bid on own auction
            } else {
                AuctionPreBidEvent event = new AuctionPreBidEvent(auction, player, amount);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    auction.placeBid(player, amount);
                }
            }
        }
    }

    @Command(
            aliases = "cancel",
            desc = "Cancel the ongoing auction.",
            max = 0
    )
    @Require("auctions.command.cancel")
    public void cancel(CommandSender sender, @Optional Auction auction) {
        MessageHandler handler = manager.getMessageHandler();

        if (auction == null) {
            // No auction
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (sender instanceof Player && manager.getMessageHandler().isIgnoring(sender)) {
            // Ignoring
            handler.sendMessage(sender, plugin.getMessage("messages.error.currentlyIgnoring"));
        } else if (auction.getTimeLeft() < plugin.getConfig().getInt("auctionSettings.mustCancelBefore", 15)
                && !sender.hasPermission("auctions.bypass.cancel.timer")) {
            // Can't cancel
            handler.sendMessage(sender, plugin.getMessage("messages.error.cantCancelNow"));
        } else if (plugin.getConfig().getInt("auctionSettings.mustCancelAfter", -1) != -1
                && auction.getTimeLeft() > plugin.getConfig().getInt("auctionSettings.mustCancelAfter", -1)
                && !sender.hasPermission("auctions.bypass.cancel.timer")) {
            // Can't cancel
            handler.sendMessage(sender, plugin.getMessage("messages.error.cantCancelNow"));
        } else if (sender instanceof Player
                && !auction.getOwner().equals(((Player) sender).getUniqueId())
                && !sender.hasPermission("auctions.bypass.cancel.otherauctions")) {
            // Can't cancel other peoples auction
            handler.sendMessage(sender, plugin.getMessage("messages.error.notYourAuction"));
        } else {
            auction.cancel();
        }
    }

    @Command(
            aliases = "end",
            desc = "End the current auction.",
            max = 0
    )
    @Require("auctions.command.end")
    public void end(CommandSender sender, @Optional Auction auction) {
        if (auction == null) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (!sender.hasPermission("auctions.bypass.end.otherauctions")
                && sender instanceof Player
                && !auction.getOwner().equals(((Player) sender).getUniqueId())) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.notYourAuction"));
        } else {
            auction.end(true);
            manager.setCurrentAuction(null);
        }
    }

    @Command(
            aliases = "ignore",
            desc = "Ignore and un-ignore auction broadcasts.",
            max = 0
    )
    @Require("auctions.command.ignore")
    public void ignore(Player player) {
        MessageHandler handler = plugin.getManager().getMessageHandler();

        if (handler.isIgnoring(player)) {
            handler.removeIgnoring(player);
            handler.sendMessage(player, plugin.getMessage("messages.noLongerIgnoring"));
        } else {
            handler.addIgnoring(player);
            handler.sendMessage(player, plugin.getMessage("messages.nowIgnoring"));
        }
    }

    @Command(
            aliases = "info",
            desc = "View information about the current auction.",
            max = 0
    )
    @Require("auctions.command.info")
    public void info(CommandSender sender, @Optional Auction auction) {
        if (auction == null) {
            plugin.getManager().getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else {
            plugin.getManager().getMessageHandler().sendAuctionInformation(sender, auction);
        }
    }

    @Command(
            aliases = "queue",
            desc = "View what auctions are currently in the queue.",
            max = 0
    )
    @Require("auctions.command.queue")
    public void queue(CommandSender sender) {
        AuctionManager manager = plugin.getManager();
        Queue<Auction> queue = manager.getQueue();

        if (queue.isEmpty()) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noAuctionsInQueue"));
        } else {
            int queuePosition = 1;

            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.queueInfoHeader"));
            for (Auction auction : queue) {
                String message = plugin.getMessage("messages.auctionFormattable.queueInfoLine")
                        .replace("[queuepos]", Integer.toString(queuePosition));
                manager.getMessageHandler().sendMessage(sender, message, auction);
                queuePosition++;
            }
        }
    }

    @Command(
            aliases = "reload",
            desc = "Reload the configuration file of the Auctions plugin.",
            max = 0
    )
    @Require("auctions.command.reload")
    public void reload(CommandSender sender) {
        plugin.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.pluginReloaded"));
        plugin.reloadConfig();
        // TODO: Reload items.yml
    }

    @Command(
            aliases = "spam",
            desc = "Ignore and un-ignore spammy messages.",
            max = 0
    )
    @Require("auctions.command.spam")
    public void spam(Player player) {
        if (!(plugin.getMessageHandler() instanceof MessageHandlerAddon.SpammyMessagePreventer)) {
            plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantHideSpam"));
        } else {
            MessageHandlerAddon.SpammyMessagePreventer preventer = (MessageHandlerAddon.SpammyMessagePreventer) plugin.getMessageHandler();

            if (!preventer.isIgnoringSpam(player.getUniqueId())) {
                preventer.addIgnoringSpam(player.getUniqueId());
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.nowHidingSpam"));
            } else {
                preventer.removeIgnoringSpam(player.getUniqueId());
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.noLongerHidingSpam"));
            }
        }
    }

    @Command(
            aliases = "start",
            usage = "<items> <price> [increment] [autowin]",
            desc = "Create a new auction using the item in your hand.",
            min = 2,
            max = 4
    )
    @Require("auctions.command.start")
    public void start(Player player, int numItems, double startingPrice, @Optional Double bidIncrement, @Optional Double autoWinPrice) {
        MessageHandler handler = plugin.getManager().getMessageHandler();

        if (plugin.getManager().isAuctioningDisabled() && !player.hasPermission("auctions.bypass.general.disabled")) {
            handler.sendMessage(player, plugin.getMessage("messages.error.auctionsDisabled"));
        } else if (!player.hasPermission("auctions.bypass.general.disabledworld")
                && plugin.isWorldDisabled(player.getWorld())) {
            handler.sendMessage(player, plugin.getMessage("messages.error.cantUsePluginInWorld"));
        } else {
            double fee = plugin.getConfig().getDouble("auctionSettings.startFee", 0);

            if (handler.isIgnoring(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.currentlyIgnoring")); // player is ignoring
            } else if (fee > plugin.getEconomy().getBalance(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.insufficientBalance")); // not enough funds
            } else if (player.getGameMode() == GameMode.CREATIVE
                    && !plugin.getConfig().getBoolean("auctionSettings.canAuctionInCreative", false)
                    && !player.hasPermission("auctions.bypass.general.creative")) {
                handler.sendMessage(player, plugin.getMessage("messages.error.creativeNotAllowed"));
            } else {
                Auction.Builder builder = new StandardAuction.StandardAuctionBuilder(plugin);

                if (bidIncrement != null) {
                    if (!plugin.getConfig().getBoolean("auctionSettings.incrementCanExceedStartPrice")
                            && bidIncrement > startingPrice) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.biddingIncrementExceedsStart"));
                        return;
                    }
                }

                if (autoWinPrice != null) {
                    if (autoWinPrice < 0) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.invalidNumberEntered")); // negative amount
                        return;
                    } else if (!player.hasPermission("auctions.bypass.start.maxautowin")
                            && autoWinPrice > plugin.getConfig().getDouble("auctionSettings.maximumAutowinAmount", 1000000D)) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.autowinTooHigh"));
                        return;
                    }
                }

                if (numItems <= 0) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.invalidNumberEntered")); // negative amount
                } else if (numItems > 2304) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.notEnoughOfItem")); // not enough
                } else if (Double.isInfinite(startingPrice) || Double.isNaN(startingPrice) || (autoWinPrice != null && (Double.isInfinite(autoWinPrice) || Double.isNaN(autoWinPrice)))) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.invalidNumberEntered")); // invalid number
                } else if (startingPrice < plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.startPriceTooLow")); // starting price too low
                } else if (!player.hasPermission("auctions.bypass.start.maxprice")
                        && startingPrice > plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.startPriceTooHigh")); // starting price too high
                } else if (plugin.getManager().getQueue().size() >= plugin.getConfig().getInt("auctionSettings.auctionQueueLimit", 3)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.auctionQueueFull")); // queue full
                } else if (bidIncrement != null && (bidIncrement < plugin.getConfig().getInt("auctionSettings.minimumBidIncrement", 10)
                        || bidIncrement > plugin.getConfig().getInt("auctionSettings.maximumBidIncrement", 9999))) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.invalidBidIncrement"));
                } else if (autoWinPrice != null && !plugin.getConfig().getBoolean("auctionSettings.canSpecifyAutowin", true)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.autowinDisabled"));
                } else if (autoWinPrice != null && Double.compare(autoWinPrice, startingPrice) <= 0) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.autowinBelowStart"));
                } else if (plugin.getManager().hasActiveAuction(player)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.alreadyHaveAuction"));
                } else if (plugin.getManager().hasAuctionInQueue(player)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.alreadyInAuctionQueue"));
                } else {
                    ItemStack item = player.getItemInHand().clone();

                    if (item == null || item.getType() == Material.AIR) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.invalidItemType")); // auctioned nothing
                    } else if (!player.hasPermission("auctions.bypass.general.bannedmaterial")
                            && plugin.getManager().isBannedMaterial(item.getType())) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.invalidItemType")); // item type not allowed
                    } else if (!player.hasPermission("auctions.bypass.general.damageditems")
                            && item.getType().getMaxDurability() > 0 && item.getDurability() > 0
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionDamagedItems", true)) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionDamagedItems")); // can't auction damaged
                    } else if (AuctionUtil.getAmountItems(player.getInventory(), item) < numItems) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.notEnoughOfItem"));
                    } else if (!player.hasPermission("auctions.bypass.general.nameditems")
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionNamedItems", true)
                            && item.getItemMeta().hasDisplayName()) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionNamedItems")); // cant auction named
                    } else if (!player.hasPermission("auctions.bypass.general.bannedlore") && hasBannedLore(item)) {
                        // The players item contains a piece of denied lore
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionBannedLore"));
                    } else {
                        item.setAmount(numItems);
                        Reward reward = new ItemReward(plugin, item);
                        if (bidIncrement != null) {
                            builder.bidIncrement(bidIncrement);
                        }
                        if (autoWinPrice != null) {
                            builder.autowin(autoWinPrice);
                        }
                        builder.reward(reward)
                                .owner(player)
                                .topBid(startingPrice);
                        Auction created = builder.build();

                        // check if we can add an autowin module
                        if (created.getAutowin() > 0 && autoWinPrice != null) {
                            created.addModule(new AutoWinModule(plugin, created, autoWinPrice));
                        }

                        // check if we can add an anti snipe module
                        if (plugin.getConfig().getBoolean("auctionSettings.antiSnipe.enable", true)) {
                            created.addModule(new AntiSnipeModule(plugin, created));
                        }

                        AuctionCreateEvent event = new AuctionCreateEvent(created, player);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            return;
                        }

                        player.getInventory().removeItem(item); // take the item from the player
                        plugin.getEconomy().withdrawPlayer(player, fee); // withdraw the start fee

                        if (plugin.getManager().canStartNewAuction()) {
                            plugin.getManager().setCurrentAuction(created);
                            created.start();
                            plugin.getManager().setCanStartNewAuction(false);
                        } else {
                            plugin.getManager().addAuctionToQueue(created);
                            handler.sendMessage(player, plugin.getMessage("messages.auctionPlacedInQueue"));
                        }
                    }
                }
            }
        }

    }

    @Command(
            aliases = "toggle",
            desc = "Toggle global state of auction creation.",
            max = 0
    )
    @Require("auctions.command.toggle")
    public void toggle(CommandSender sender) {
        plugin.getManager().setAuctioningDisabled(!plugin.getManager().isAuctioningDisabled());
        String message = plugin.getManager().isAuctioningDisabled() ? "messages.auctionsDisabled" : "messages.auctionsEnabled";
        plugin.getManager().getMessageHandler().broadcast(plugin.getMessage(message), false);
    }

    /**
     * Checks if an item has a banned piece of lore
     *
     * @param item the item
     * @return true if the item has a banned piece of lore
     */
    public boolean hasBannedLore(ItemStack item) {
        List<String> bannedLore = plugin.getConfig().getStringList("general.blockedLore");

        if (bannedLore != null && !bannedLore.isEmpty()) {
            if (item.getItemMeta().hasLore()) {
                List<String> lore = item.getItemMeta().getLore();

                for (String loreItem : lore) {
                    for (String banned : bannedLore) {
                        if (loreItem.contains(banned)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}

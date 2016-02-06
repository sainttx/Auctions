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

import com.sainttx.auctions.MessagePath;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.MessageFactory;
import com.sainttx.auctions.api.event.AuctionCreateEvent;
import com.sainttx.auctions.api.event.AuctionPreBidEvent;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.messages.MessageHandlerAddon;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.structure.auction.StandardAuction;
import com.sainttx.auctions.structure.module.AntiSnipeModule;
import com.sainttx.auctions.structure.module.AutoWinModule;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.argument.MissingArgumentException;
import com.sk89q.intake.parametric.annotation.Optional;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionCommands {

    private final AuctionPlugin plugin;
    private final AuctionManager manager;
    private final MessageFactory messageFactory;

    public AuctionCommands(final AuctionPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager();
        this.messageFactory = plugin.getMessageFactory();
    }

    @Command(
            aliases = "impound",
            desc = "Impound the currently active auction.",
            max = 0
    )
    @Require("auctions.command.impound")
    public void impound(CommandSender sender, @Optional Auction auction) {
        if (!(sender instanceof Player)) { // TODO: Allow console to impound?
            sender.sendMessage("Only players can impound auctions");
        } else if (auction == null) {
            messageFactory.submit(sender, MessagePath.ERROR_NO_AUCTION);
        } else {
            Player player = (Player) sender;
            Reward reward = auction.getReward();
            auction.impound();
            reward.giveItem(player);

            // TODO: [player] placeholder
            // TODO: broadcast this message
            messageFactory.submit(player, MessagePath.GENERAL_AUCTION_IMPOUNDED);
        }
    }

    @Command(
            aliases = "bid",
            usage = "[amount]",
            desc = "Bid on the currently active auction.",
            max = 1
    )
    @Require("auctions.command.bid")
    public void bid(Player player, @Optional Auction auction, @Optional Double amount) throws MissingArgumentException {
        if (amount == null && !plugin.getSettings().canBidAutomatically()) {
            throw new MissingArgumentException(); // TODO: Check if this is valid
        } else if (auction == null) {
            messageFactory.submit(player, MessagePath.ERROR_NO_AUCTION);
        } else {
            if (!player.hasPermission("auctions.bypass.general.disabledworld")
                    && plugin.getSettings().isDisabledWorld(player.getWorld().getName())) {
                messageFactory.submit(player, MessagePath.ERROR_DISABLED_WORLD);
            } else if (manager.getMessageHandler().isIgnoring(player)) {
                messageFactory.submit(player, MessagePath.ERROR_IGNORING);
            } else if (auction.getOwner().equals(player.getUniqueId())) {
                messageFactory.submit(player, MessagePath.ERROR_OWN_AUCTION);
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
            messageFactory.submit(sender, MessagePath.ERROR_NO_AUCTION);
        } else if (sender instanceof Player && manager.getMessageHandler().isIgnoring(sender)) {
            // Ignoring
            messageFactory.submit(sender, MessagePath.ERROR_IGNORING);
        } else if (auction.getTimeLeft() < plugin.getSettings().getMustCancelBeforeTime() // TODO: Check
                && !sender.hasPermission("auctions.bypass.cancel.timer")) {
            // Can't cancel
            messageFactory.submit(sender, MessagePath.ERROR_CANT_CANCEL);
        } else if (plugin.getSettings().getMustCancelAfterTime() != -1
                && auction.getTimeLeft() > plugin.getSettings().getMustCancelAfterTime() // TODO: Check
                && !sender.hasPermission("auctions.bypass.cancel.timer")) {
            // Can't cancel
            messageFactory.submit(sender, MessagePath.ERROR_CANT_CANCEL);
        } else if (sender instanceof Player
                && !auction.getOwner().equals(((Player) sender).getUniqueId())
                && !sender.hasPermission("auctions.bypass.cancel.otherauctions")) {
            // Can't cancel other peoples auction
            messageFactory.submit(sender, MessagePath.ERROR_OTHER_AUCTION);
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
            messageFactory.submit(sender, MessagePath.ERROR_NO_AUCTION);
        } else if (!sender.hasPermission("auctions.bypass.end.otherauctions")
                && sender instanceof Player
                && !auction.getOwner().equals(((Player) sender).getUniqueId())) {
            messageFactory.submit(sender, MessagePath.ERROR_OTHER_AUCTION);
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
            plugin.getMessageFactory().submit(player, MessagePath.GENERAL_ENABLE_MESSAGES);
        } else {
            handler.addIgnoring(player);
            plugin.getMessageFactory().submit(player, MessagePath.GENERAL_DISABLE_MESSAGES);
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
            messageFactory.submit(sender, MessagePath.ERROR_NO_AUCTION);
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
        if (manager.getQueue().isEmpty()) {
            messageFactory.submit(sender, MessagePath.ERROR_QUEUE_EMPTY);
        } else {
            messageFactory.submit(sender, MessagePath.GENERAL_QUEUE_HEADER);
            // TODO: Change [queuepos] to auction format somehow
            manager.getQueue()
                    .forEach(auction -> messageFactory.submit(sender, MessagePath.AUCTION_QUEUE_POSITION, auction));
        }
    }

    @Command(
            aliases = "reload",
            desc = "Reload the configuration file of the Auctions plugin.",
            max = 0
    )
    @Require("auctions.command.reload")
    public void reload(CommandSender sender) {
        messageFactory.submit(sender, MessagePath.GENERAL_PLUGIN_RELOAD);
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
        // TODO: Spam will always be preventable in the future
        if (!(plugin.getMessageHandler() instanceof MessageHandlerAddon.SpammyMessagePreventer)) {
            // TODO: Remove this message
            // plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantHideSpam"));
        } else {
            MessageHandlerAddon.SpammyMessagePreventer preventer = (MessageHandlerAddon.SpammyMessagePreventer) plugin.getMessageHandler();

            if (!preventer.isIgnoringSpam(player.getUniqueId())) {
                preventer.addIgnoringSpam(player.getUniqueId());
                messageFactory.submit(player, MessagePath.GENERAL_DISABLE_SPAM);
            } else {
                preventer.removeIgnoringSpam(player.getUniqueId());
                messageFactory.submit(player, MessagePath.GENERAL_ENABLE_SPAM);
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
            messageFactory.submit(player, MessagePath.ERROR_DISABLED);
        } else if (!player.hasPermission("auctions.bypass.general.disabledworld")
                && plugin.getSettings().isDisabledWorld(player.getWorld().getName())) {
            messageFactory.submit(player, MessagePath.ERROR_DISABLED_WORLD);
        } else {
            if (handler.isIgnoring(player)) {
                messageFactory.submit(player, MessagePath.ERROR_IGNORING); // player is ignoring
            } else if (plugin.getSettings().getStartFee() > plugin.getEconomy().getBalance(player)) { // TODO: Double.compare
                messageFactory.submit(player, MessagePath.ERROR_MONEY); // not enough funds
            } else if (player.getGameMode() == GameMode.CREATIVE
                    && !plugin.getSettings().canAuctionInCreative()
                    && !player.hasPermission("auctions.bypass.general.creative")) {
                messageFactory.submit(player, MessagePath.ERROR_CREATIVE);
            } else {
                Auction.Builder builder = new StandardAuction.StandardAuctionBuilder(plugin);

                if (bidIncrement != null) {
                    if (!plugin.getSettings().canBidIncrementExceedStartPrice()
                            && bidIncrement > startingPrice) {
                        messageFactory.submit(player, MessagePath.ERROR_INCREMENT_EXCEEDS);
                        return;
                    }
                }

                if (autoWinPrice != null) {
                    if (autoWinPrice < 0) {
                        messageFactory.submit(player, MessagePath.ERROR_INVALID_NUMBER);
                        return;
                    } else if (!player.hasPermission("auctions.bypass.start.maxautowin")
                            && autoWinPrice > plugin.getSettings().getMaximumAutowin()) { // TODO: double.compare
                        messageFactory.submit(player, MessagePath.ERROR_AUTOWIN_TOOHIGH);
                        return;
                    }
                }

                if (numItems <= 0) {
                    messageFactory.submit(player, MessagePath.ERROR_INVALID_NUMBER);
                } else if (numItems > 2304) {
                    messageFactory.submit(player, MessagePath.ERROR_NOT_ENOUGH_ITEM);
                } else if (Double.isInfinite(startingPrice) || Double.isNaN(startingPrice) || (autoWinPrice != null && (Double.isInfinite(autoWinPrice) || Double.isNaN(autoWinPrice)))) {
                    messageFactory.submit(player, MessagePath.ERROR_INVALID_NUMBER);
                } else if (startingPrice < plugin.getSettings().getMinimumStartPrice()) { // TODO: Double.compare
                    messageFactory.submit(player, MessagePath.ERROR_STARTPRICE_LOW);
                } else if (!player.hasPermission("auctions.bypass.start.maxprice")
                        && startingPrice > plugin.getSettings().getMaximumStartPrice()) {  // TODO: Double.compare
                    messageFactory.submit(player, MessagePath.ERROR_STARTPRICE_HIGH);
                } else if (plugin.getManager().getQueue().size() >= plugin.getSettings().getAuctionQueueLimit()) {
                    messageFactory.submit(player, MessagePath.ERROR_QUEUE_FULL);
                } else if (bidIncrement != null && (bidIncrement < plugin.getSettings().getMinimumBidIncrement()  // TODO: Double.compare
                        || bidIncrement > plugin.getSettings().getMaximumBidIncrement())) {  // TODO: Double.compare
                    messageFactory.submit(player, MessagePath.ERROR_INCREMENT_INVALID);
                } else if (autoWinPrice != null && !plugin.getSettings().canSpecifyAutowin()) {
                    messageFactory.submit(player, MessagePath.ERROR_AUTOWIN_DISABLED);
                } else if (autoWinPrice != null && Double.compare(autoWinPrice, startingPrice) <= 0) {
                    messageFactory.submit(player, MessagePath.ERROR_AUTOWIN_BELOW_START);
                } else if (plugin.getManager().hasActiveAuction(player)) {
                    messageFactory.submit(player, MessagePath.ERROR_ALREADY_AUCTIONING);
                } else if (plugin.getManager().hasAuctionInQueue(player)) {
                    messageFactory.submit(player, MessagePath.ERROR_IN_QUEUE);
                } else {
                    ItemStack item = player.getItemInHand().clone();

                    if (item == null || item.getType() == Material.AIR) {
                        messageFactory.submit(player, MessagePath.ERROR_ITEM_INVALID);
                    } else if (!player.hasPermission("auctions.bypass.general.bannedmaterial")
                            && plugin.getSettings().isBlockedMaterial(item.getType())) {
                        messageFactory.submit(player, MessagePath.ERROR_ITEM_INVALID);
                    } else if (!player.hasPermission("auctions.bypass.general.damageditems")
                            && item.getType().getMaxDurability() > 0 && item.getDurability() > 0
                            && !plugin.getSettings().canAuctionDamagedItems()) {
                        messageFactory.submit(player, MessagePath.ERROR_DAMAGED_ITEM);
                    } else if (!player.getInventory().containsAtLeast(item, numItems)) {
                        messageFactory.submit(player, MessagePath.ERROR_NOT_ENOUGH_ITEM);
                    } else if (!player.hasPermission("auctions.bypass.general.nameditems")
                            && !plugin.getSettings().canAuctionNamedItems()
                            && item.getItemMeta().hasDisplayName()) {
                        messageFactory.submit(player, MessagePath.ERROR_NAMED_ITEM);
                    } else if (!player.hasPermission("auctions.bypass.general.bannedlore") && hasBannedLore(item)) {
                        // The players item contains a piece of denied lore
                        messageFactory.submit(player, MessagePath.ERROR_BANNED_LORE);
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
                        if (plugin.getSettings().isAntiSnipeEnabled()) {
                            created.addModule(new AntiSnipeModule(plugin, created));
                        }

                        AuctionCreateEvent event = new AuctionCreateEvent(created, player);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            return;
                        }

                        player.getInventory().removeItem(item); // take the item from the player
                        plugin.getEconomy().withdrawPlayer(player, plugin.getSettings().getStartFee()); // withdraw the start fee

                        if (plugin.getManager().canStartNewAuction()) {
                            plugin.getManager().setCurrentAuction(created);
                            created.start();
                            plugin.getManager().setCanStartNewAuction(false);
                        } else {
                            plugin.getManager().addAuctionToQueue(created);
                            messageFactory.submit(player, MessagePath.GENERAL_PLACED_IN_QUEUE);
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
        manager.setAuctioningDisabled(!manager.isAuctioningDisabled());
        // TODO: Broadcast this message
        messageFactory.submit(sender, manager.isAuctioningDisabled() ? MessagePath.GENERAL_DISABLED : MessagePath.GENERAL_ENABLED);
    }

    /**
     * Checks if an item has a banned piece of lore
     *
     * @param item the item
     * @return true if the item has a banned piece of lore
     */
    public boolean hasBannedLore(ItemStack item) {
        if (item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();

            for (String loreItem : lore) {
                if (plugin.getSettings().isBlockedLore(loreItem)) {
                    return true;
                }
            }
        }

        return false;
    }
}

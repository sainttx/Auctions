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

package com.sainttx.auctions.api;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface Settings {

    /**
     * TODO LIST:
     * - Message groups
     * - Rework handlers for async message creation (MessageFactory/Enum?)
     */

    /**
     * Returns the amount of money anybody starting an {@link Auction} are to be charged.
     *
     * @return the amount of money specified
     */
    double getStartFee();

    /**
     * Returns the percentage variable to calculate how much money to remove from the
     * winnings of an {@link Auction}. For example, if an auction is won for $1,000.00
     * and the tax percentage is set to {@code 30.0}%, $300.00 will be removed from the
     * pot and the owner of the {@link Auction} will be given $700.00 as a result.
     *
     * @return the tax percentage
     */
    double getTaxPercent();

    /**
     * Returns the start time, in seconds, for all {@link Auction}s.
     *
     * @return the start time
     */
    int getStartTime();

    /**
     * Returns the delay between the time an {@link Auction} ends and the time a new
     * {@link Auction} is to be started by the {@link AuctionManager} instance.
     *
     * @return the delay time
     */
    int getDelayBetweenAuctions();

    /**
     * Returns the default bid increment to set on newly created {@link Auction} objects
     * that have not had an increment specified.
     *
     * @return the default increment
     */
    double getDefaultBidIncrement();

    /**
     * Returns the minimum possible bid increment that is allowed to be specified on
     * an {@link Auction}.
     *
     * @return the minimum increment.
     */
    double getMinimumBidIncrement();

    /**
     * Returns the maximum possible bid increment that is allowed to be specified on
     * an {@link Auction}.
     *
     * @return the maximum increment.
     */
    double getMaximumBidIncrement();

    /**
     * Returns whether or not an {@link Auction} can be created (via commands) with a
     * bid increment that exceeds the initial starting price.
     * <p>
     * For example, if an {@link Auction} is created with a start price of $1,000.00
     * then this setting defines whether the bid increment provided can exceed the
     * initial value of $1,000.00 (ie. $1500.00).
     *
     * @return {@code true} if the bid increment can exceed the starting price of an
     * {@link Auction}, {@code false} otherwise.
     */
    boolean canBidIncrementExceedStartPrice();

    /**
     * Returns the minimum possible starting amount that is allowed to be specific on
     * an {@link Auction}.
     *
     * @return the minimum start price.
     */
    double getMinimumStartPrice();

    /**
     * Returns the maximum possible starting amount that is allowed to be specific on
     * an {@link Auction}.
     *
     * @return the maximum start price.
     */
    double getMaximumStartPrice();

    /**
     * Returns the maximum possible auto-win amount that is allowed to be specific on
     * an {@link Auction}.
     *
     * @return the maximum auto-win amount.
     */
    double getMaximumAutowin();

    /**
     * Returns whether or not named {@link ItemStack}s can be auctioned.
     *
     * @return {@code true} if named {@link ItemStack}s can be auctioned.
     */
    boolean canAuctionNamedItems();

    /**
     * Returns whether or not {@link ItemStack}s that have a damaged durability can be auctioned.
     *
     * @return {@code true} if damaged {@link ItemStack}s can be auctioned.
     */
    boolean canAuctionDamagedItems();

    /**
     * Returns whether or not an auto-win amount can be specified in {@link Auction}s.
     *
     * @return {@code true} if auto-win amounts are allowed.
     */
    boolean canSpecifyAutowin();

    /**
     * Returns whether or not {@link Player}s can create {@link Auction}s in {@link GameMode#CREATIVE}.
     *
     * @return {@code true} if {@link GameMode#CREATIVE} auctioning is allowed.
     */
    boolean canAuctionInCreative();

    /**
     * Returns whether or not players can use the /bid command with no arguments provided. The bid amount
     * will automatically be set to the highest current bid amount added with the increment.
     *
     * @return {@code true} if automatic bidding is allowed.
     */
    boolean canBidAutomatically();

    /**
     * Returns the maximum number of {@link Auction}s that are allowed to be in the queue provided by {@link AuctionManager}.
     *
     * @return the maximum queued {@link Auction}s.
     */
    int getAuctionQueueLimit();

    /**
     * Returns the time, in seconds, that an {@link Auction} must be cancelled before.
     * <p>
     * For example, if this value is set to be {@code 15} seconds then an auction must be
     * cancelled in the first 15 seconds.
     *
     * @return the time limit to cancel auctions
     */
    int getMustCancelBeforeTime();

    /**
     * Returns the time, in seconds, that an {@link Auction} must be cancelled after.
     * <p>
     * For example, if this value is set to be {@code 5} seconds then an auction cannot be
     * cancelled in the first 15 seconds.
     *
     * @return the time limit to cancel auctions
     */
    int getMustCancelAfterTime();

    /**
     * Returns whether anti-snipe is enabled for auctions.
     *
     * @return {@code true} if anti-snipe is enabled.
     */
    boolean isAntiSnipeEnabled();

    /**
     * Returns the time left, in seconds, which an {@link Auction} that has anti-snipe can be triggered at.
     * <p>
     * For example, if this value is set to be {@code 3}, then anti-snipe will only trigger if a bid
     * is placed in the last 3 seconds of the auction (inclusive).
     *
     * @return the anti-snipe time threshold
     */
    int getAntiSnipeTimeThreshold();

    /**
     * Returns the maximum amount of anti-snipes allowed per {@link Auction}.
     *
     * @return the maximum amount of anti-snipes
     */
    int getMaximumAntiSnipesPerAuction();

    /**
     * Returns the extra time, in seconds, to be added to an {@link Auction} whenever anti-snipe is triggered.
     *
     * @return the extra time
     */
    int getAntiSnipeExtraTime();

    /**
     * Returns whether or not any commands should be ran after an {@link Auction} ends.
     *
     * @return {@code true} if commands should be ran when an auction ends.
     */
    boolean shouldRunPostAuctionCommands();

    /**
     * Returns whether or not commands should be ran after an {@link Auction} only if the item was sold.
     *
     * @return {@code true} if commands should only be ran if item was sold.
     */
    boolean shouldRunPostAuctionCommandsOnlyIfSold();

    /**
     * Returns any commands to run after an {@link Auction} completes.
     *
     * @return a list of commands.
     */
    List<String> getPostAuctionCommands();

    /**
     * Returns the delay for when a players offline reward is to be given back when they log in.
     *
     * @return the delay.
     */
    int getOfflineRewardTickDelay();

    /**
     * Returns whether or not to shorten formatted times in any messages.
     * <p>
     * The default time format is "5 minutes 5 seconds". Times that are shorted appear as "5m, 5s".
     *
     * @return {@code true} if times should be shortened.
     */
    boolean shouldUseShortenedTimes();

    /**
     * Returns whether or not to truncate formatted numbers.
     * <p>
     * Numbers that are un-truncated are in the format 5,000,000 versus truncated which appear as 5M.
     *
     * @return {@code true} if numbers should be truncated.
     */
    boolean shouldTruncateNumbers();

    /**
     * Returns whether or not to strip display name color from any {@link ItemStack}s using {@link ChatColor#translateAlternateColorCodes(char, String)}.
     *
     * @return {@code true} if display name colors should be stripped.
     */
    boolean shouldStripDisplayNameColor();

    /**
     * Returns whether or not {@link Auction}s can be started in a {@link World}.
     *
     * @param world the name of the world.
     * @return {@code true} if the {@link World} is disabled.
     */
    boolean isDisabledWorld(String world);

    /**
     * Returns whether or not commands should be blocked if a {@link Player} is currently the owner of an {@link Auction}.
     *
     * @return {@code true} if commands are to be blocked.
     */
    boolean shouldBlockCommandsIfAuctioning();

    /**
     * Returns whether or not commands should be blocked if a {@link Player} currently has an {@link Auction} in the queue.
     *
     * @return {@code true} if commands are to be blocked.
     */
    boolean shouldBlockCommandsIfQueued();

    /**
     * Returns whether or not commands should be blocked if a {@link Player} is currently a top bidder of an {@link Auction}.
     *
     * @return {@code true} if commands are to be blocked.
     */
    boolean shouldBlockCommandsIfTopBidder();

    /**
     * Returns whether or not a command is blocked.
     *
     * @param command the name of the command.
     * @return {@code true} if the command is blocked.
     */
    boolean isBlockedCommand(String command);

    /**
     * Returns whether or not a {@link Material} is blocked.
     *
     * @param material the material
     * @return {@code true} if the material is not allowed to be auctioned.
     */
    boolean isBlockedMaterial(Material material);

    /**
     * Returns whether or not a piece of lore from an {@link ItemStack} is blocked.
     *
     * @param lore the String of lore
     * @return {@code true} if the lore is blocked.
     */
    boolean isBlockedLore(String lore);

    /**
     * Returns whether or not a time should issue a broadcast announcement.
     *
     * @param time the time in seconds
     * @return true if the time is defined.
     */
    boolean isBroadcastTime(int time);
}

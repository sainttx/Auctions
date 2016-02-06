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

package com.sainttx.auctions;

import com.sainttx.auctions.api.Message;

public enum MessagePath implements Message {
    // Auction formattable messages
    AUCTION_ITEMFORMAT("messages.auctionFormattable.itemFormat"),
    AUCTION_ANTISNIPE_ADD("messages.auctionFormattable.antiSnipeAdd"),
    AUCTION_AUTOWIN("messages.auctionFormattable.autowin"),
    AUCTION_BID("messages.auctionFormattable.bid", true),
    AUCTION_CANCELLED("messages.auctionFormattable.cancelled"),
    AUCTION_END("messages.auctionFormattable.end"),
    AUCTION_END_AUTOWIN("messages.auctionFormattable.endByAutowin"),
    AUCTION_END_NOBID("messages.auctionFormattable.endNoBid"),
    AUCTION_END_OWNERMSG("messages.auctionFormattable.endNotifyOwner"),
    AUCTION_END_TAX("messages.auctionFormattable.endTax"),
    AUCTION_INCREMENT("messages.auctionFormattable.increment"),
    AUCTION_INFO("messages.auctionFormattable.info"),
    AUCTION_INFO_TOPBIDDER("messages.auctionFormattable.infoTopBidder"),
    AUCTION_PRICE("messages.auctionFormattable.price"),
    AUCTION_QUEUE_INFO("messages.auctionFormattable.queueInfoLine"),
    AUCTION_QUEUE_POSITION("messages.auctionFormattable.queuePosition"),
    AUCTION_START("messages.auctionFormattable.start"),
    AUCTION_TIMER("messages.auctionFormattable.timer", true),
    AUCTION_WINNER("messages.auctionFormattable.winner"),

    // Errors - player mistakes
    ERROR_ALREADY_AUCTIONING("messages.error.alreadyHaveAuction"),
    ERROR_IN_QUEUE("messages.error.alreadyInAuctionQueue"),
    ERROR_TOP_BIDDER("messages.error.alreadyTopBidder"),
    ERROR_QUEUE_FULL("messages.error.auctionQueueFull"),
    ERROR_DISABLED("messages.error.auctionsDisabled"),
    ERROR_AUTOWIN_BELOW_START("messages.error.autowinBelowStart"),
    ERROR_AUTOWIN_DISABLED("messages.error.autowinDisabled"),
    ERROR_AUTOWIN_TOOHIGH("messages.error.autowinTooHigh"),
    ERROR_INCREMENT_EXCEEDS("messages.error.biddingIncrementExceedsStart"),
    ERROR_OWN_AUCTION("messages.error.bidOnOwnAuction"),
    ERROR_BID_LOW("messages.error.bidTooLow"),
    ERROR_BANNED_LORE("messages.error.cantAuctionBannedLore"),
    ERROR_DAMAGED_ITEM("messages.error.cantAuctionDamagedItems"),
    ERROR_NAMED_ITEM("messages.error.cantAuctionNamedItems"),
    ERROR_CANT_CANCEL("messages.error.cantCancelNow"),
    ERROR_DISABLED_TELEPORT("messages.error.cantTeleportToDisabledWorld"),
    ERROR_COMMAND_AUCTIONING("messages.error.cantUseCommandWhileAuctioning"),
    ERROR_COMMAND_QUEUE("messages.error.cantUseCommandWhileQueued"),
    ERROR_COMMAND_TOPBIDDER("messages.error.cantUseCommandWhileTopBidder"),
    ERROR_DISABLED_WORLD("messages.error.cantUsePluginInWorld"),
    ERROR_CREATIVE("messages.error.creativeNotAllowed"),
    ERROR_IGNORING("messages.error.currentlyIgnoring"),
    ERROR_MONEY("messages.error.insufficientBalance"),
    ERROR_PERMISSIONS("messages.error.insufficientPermissions"),
    ERROR_INCREMENT_INVALID("messages.error.invalidBidIncrement"),
    ERROR_ITEM_INVALID("messages.error.invalidItemType"),
    ERROR_INVALID_NUMBER("messages.error.invalidNumberEntered"),
    ERROR_QUEUE_EMPTY("messages.error.noAuctionsInQueue"),
    ERROR_NO_AUCTION("messages.error.noCurrentAuction"),
    ERROR_NOT_ENOUGH_ITEM("messages.error.notEnoughOfItem"),
    ERROR_OTHER_AUCTION("messages.error.notYourAuction"),
    ERROR_STARTPRICE_HIGH("messages.error.startPriceTooHigh"),
    ERROR_STARTPRICE_LOW("messages.error.startPriceTooLow"),

    // General messages
    GENERAL_AUCTION_IMPOUNDED("messages.auctionImpounded"), // TODO: Should be auction formattable, [player] placeholder wont work
    GENERAL_PLACED_IN_QUEUE("messages.auctionPlacedInQueue"),
    GENERAL_DISABLED("messages.auctionsDisabled"),
    GENERAL_ENABLED("messages.auctionsEnabled"),
    GENERAL_BID_PERSONAL("messages.bid"), // TODO: Auction formattable, [bid] placeholder changed if needed
    GENERAL_ENABLE_SPAM("messages.noLongerHidingSpam"),
    GENERAL_DISABLE_SPAM("messages.nowHidingSpam"),
    GENERAL_ENABLE_MESSAGES("messages.noLongerIgnoring"),
    GENERAL_DISABLE_MESSAGES("messages.nowIgnoring"),
    GENERAL_NOT_ENOUGH_ROOM("messages.notEnoughRoom"),
    GENERAL_ITEM_RETURN("messages.ownerItemReturn"),
    GENERAL_PLUGIN_RELOAD("messages.pluginReloaded"),
    GENERAL_QUEUE_HEADER("messages.queueInfoHeader"),
    GENERAL_SAVED_ITEM_RETURN("messages.savedItemReturn")
    ;

    private final String path;
    private final boolean spammy;

    MessagePath(String path) {
        this(path, false);
    }

    MessagePath(String path, boolean spammy) {
        this.path = path;
        this.spammy = spammy;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isSpammy() {
        return spammy;
    }
}

package com.sainttx.auction.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents an Auction that players can bid on
 */
public interface Auction {

    /**
     * Gets the owner of this auction
     *
     * @return the auction creator
     */
    UUID getOwner();

    /**
     * Gets the {@link UUID} of the current top bidder for this auction
     *
     * @return the current {@link UUID} of the winner
     */
    UUID getTopBidder();

    /**
     * Gets the name of the current top bidder
     *
     * @return the top bidders name
     */
    String getTopBidderName();

    /**
     * Gets the item that is being auctioned
     *
     * @return the auctioned item
     */
    ItemStack getItem();

    /**
     * Gets the amount of time left in this auction
     *
     * @return amount of time left
     */
    int getTimeLeft();

    /**
     * Sets the amount of time left in this auction
     *
     * @param time new amount of time left
     */
    void setTimeLeft(int time);

    /**
     * Cancels this auction and returns the items to the owner
     */
    void cancel();

    /**
     * Ends the auction as if the timer ran out
     *
     * @param broadcast whether or not to broadcast any
     *                  information about this auction ending
     */
    void end(boolean broadcast);

    /**
     * Gets the amount required to automatically win this auction
     *
     * @return the auto win amount
     */
    double getAutowin();

    /**
     * Gets the lowest amount that can be bid on this auction
     *
     * @return the bid increment of this auction
     */
    double getBidIncrement();

    /**
     * Gets the percentage of money that will be removed from
     * the winning amount
     *
     * @return the tax percent of this auction
     */
    double getTax();

    /**
     * Sets the top bidder of this auction
     *
     * @param bidder the new top bidder
     */
    void setTopBidder(UUID bidder);

    /**
     * Gets the current top bid in this auction
     *
     * @return the top bid
     */
    double getTopBid();

    /**
     * Sets the top bid for this auction
     *
     * @param bid the new top bid
     */
    void setTopBid(double bid);

    /**
     * Places a bid made by a player, does not handle
     * any economy functions
     *
     * @param player the player
     * @param bid    the amount bid by the player
     */
    void placeBid(Player player, double bid);

    /**
     * Represents an auctions timer
     */
    interface Timer extends Runnable {

    }

    /**
     * Represents an Auction builder
     */
    interface Builder {

        /**
         * Creates the auction
         *
         * @return the auction created by the builder
         */
        Auction build();
    }
}

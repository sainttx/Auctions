package com.sainttx.auctions.api.module;

import com.sainttx.auctions.api.Auction;

/**
 * Represents a module that can be added onto an {@link Auction}.
 * <p>
 * Added Auction module implementations are always triggered after a
 * new bid is successfully placed on an auction. An example of an
 * implementation is an AntiSnipe Module which checks if an anti snipe
 * instance can be triggered on an Auction and then adds time if it triggers.
 * </p>
 */
public interface AuctionModule {

    /**
     * Gets whether or not the module can be triggered
     *
     * @return true if the module can be triggered
     */
    boolean canTrigger();

    /**
     * Triggers the action in this module
     */
    void trigger();
}

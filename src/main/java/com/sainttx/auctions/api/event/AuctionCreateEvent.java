package com.sainttx.auctions.api.event;

import com.sainttx.auctions.api.Auction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Created by Matthew on 9/7/2015.
 */
public class AuctionCreateEvent extends AuctionEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player who;
    private boolean cancelled;

    /**
     * Called when a player creates an Auction with a command
     *
     * @param auction the created auction
     * @param who the player who created the auction
     */
    public AuctionCreateEvent(final Auction auction, Player who) {
        super(auction);
        this.who = who;
    }

    /**
     * Returns the player that created the Auction
     *
     * @return the Auction owner
     */
    public Player getPlayer() {
        return who;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
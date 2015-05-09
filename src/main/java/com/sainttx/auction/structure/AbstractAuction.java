package com.sainttx.auction.structure;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.module.AuctionModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by Matthew on 08/05/2015.
 */
public abstract class AbstractAuction implements Auction {

    /*
     * Protect from reflective instantiation
     */
    private AbstractAuction() {
        throw new IllegalAccessError("cannot create empty auction instances");
    }

    public AbstractAuction(AuctionPlugin plugin) {

    }

    @Override
    public UUID getOwner() {
        return null;
    }

    @Override
    public UUID getTopBidder() {
        return null;
    }

    @Override
    public String getTopBidderName() {
        return null;
    }

    @Override
    public ItemStack getItem() {
        return null;
    }

    @Override
    public int getTimeLeft() {
        return 0;
    }

    @Override
    public void setTimeLeft(int time) {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void end(boolean broadcast) {

    }

    @Override
    public double getAutowin() {
        return 0;
    }

    @Override
    public double getBidIncrement() {
        return 0;
    }

    @Override
    public double getTax() {
        return 0;
    }

    @Override
    public double getTopBid() {
        return 0;
    }

    @Override
    public void placeBid(Player player, double bid) {

    }

    @Override
    public Collection<AuctionModule> getModules() {
        return null;
    }

    @Override
    public void addModule(AuctionModule module) {

    }

    @Override
    public boolean removeModule(AuctionModule module) {
        return false;
    }
}

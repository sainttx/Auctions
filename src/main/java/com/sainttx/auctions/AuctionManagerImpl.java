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

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.messages.MessageRecipientGroup;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages all auction features. Only one manager instance
 * is active at all times (ie. Singleton Design Pattern).
 */
public class AuctionManagerImpl implements AuctionManager {

    // Instance
    private static AuctionManagerImpl manager = new AuctionManagerImpl();

    // Auctions information
    private Auction currentAuction;
    private Set<MessageRecipientGroup> recipientGroups = new HashSet<MessageRecipientGroup>();
    private Queue<Auction> auctionQueue = new ConcurrentLinkedQueue<Auction>();
    private Set<Material> banned = EnumSet.noneOf(Material.class);
    private MessageHandler handler;
    private boolean disabled;
    private boolean canAuction = true;

    private AuctionManagerImpl() {
        if (manager != null) {
            throw new IllegalStateException("cannot create new instances of the manager");
        }

        loadBannedMaterials();
    }

    /**
     * Singleton. Returns the auction manager instance.
     *
     * @return the only auction manager instance
     */
    public static AuctionManagerImpl getAuctionManager() {
        if (manager == null) { // Should never happen
            manager = new AuctionManagerImpl();
            Bukkit.getLogger().info("Created a new auction manager instance.");
        }

        return manager;
    }

    /**
     * Called when the Auctions plugin disables
     */
    protected static void disable() {
        if (getAuctionManager().getCurrentAuction() != null) {
            getAuctionManager().getCurrentAuction().cancel();
        }

        for (Auction auction : getAuctionManager().getQueue()) {
            auction.end(false);
        }
        getAuctionManager().getQueue().clear();

        manager = null;
    }

    @Override
    public int getQueuePosition(Player player) {
        int position = 0;

        for (Auction auction : getQueue()) {
            if (player.getUniqueId().equals(auction.getOwner())) {
                return position + 1;
            }

            position++;
        }

        return -1;
    }

    @Override
    public boolean hasAuctionInQueue(Player player) {
        for (Auction auction : getQueue()) {
            if (player.getUniqueId().equals(auction.getOwner())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasActiveAuction(Player player) {
        return getCurrentAuction() != null && player.getUniqueId().equals(getCurrentAuction().getOwner());
    }

    @Override
    public boolean isAuctioningDisabled() {
        return disabled;
    }

    @Override
    public void setAuctioningDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public Auction getCurrentAuction() {
        return currentAuction;
    }

    @Override
    public void setCurrentAuction(Auction auction) {
        this.currentAuction = auction;
    }

    @Override
    public boolean canStartNewAuction() {
        return currentAuction == null && canAuction;
    }

    @Override
    public void setCanStartNewAuction(boolean start) {
        this.canAuction = start;
    }

    @Override
    public Queue<Auction> getQueue() {
        return auctionQueue;
    }

    @Override
    public void addAuctionToQueue(Auction auction) {
        getQueue().add(auction);
    }

    @Override
    public MessageHandler getMessageHandler() {
        return handler;
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("new message handler cannot be null");
        }

        this.handler = handler;
    }

    @Override
    public void addMessageGroup(MessageRecipientGroup group) {
        recipientGroups.add(group);
    }

    @Override
    public Collection<MessageRecipientGroup> getMessageGroups() {
        return recipientGroups;
    }

    @Override
    public void startNextAuction() {
        Auction next = getQueue().poll();

        if (next != null) {
            next.start();
            currentAuction = next;
        }
    }

    @Override
    public boolean isBannedMaterial(Material material) {
        return banned.contains(material);
    }

    /* 
     * Loads all banned items into memory 
     */
    private void loadBannedMaterials() {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        if (!plugin.getConfig().isList("general.blockedMaterials")) {
            return;
        }

        for (String materialString : plugin.getConfig().getStringList("general.blockedMaterials")) {
            Material material = Material.getMaterial(materialString);

            if (material == null) {
                plugin.getLogger().info("Material \"" + materialString + "\" is not a valid Material and will not be blocked.");
            } else {
                banned.add(material);
                plugin.getLogger().info("Material \"" + material.toString() + "\" added as a blocked material.");
            }
        }
    }
}
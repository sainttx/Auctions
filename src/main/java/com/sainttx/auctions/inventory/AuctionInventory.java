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

package com.sainttx.auctions.inventory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Created by Matthew on 25/05/2015.
 */
public class AuctionInventory implements InventoryHolder {

    /*
     * The title of the Inventory
     */
    private String title;

    /*
     * The player this inventory is handling
     */
    private UUID player;

    /*
     * The start price for the auction
     */
    private double startPrice;

    /*
     * The Inventory object
     */
    private Inventory inventory;

    /**
     * Creates a new auction inventory with amount of rows and title
     *
     * @param title The title of the inventory
     * @param player The player who is adding items
     */
    public AuctionInventory(String title, UUID player, double startPrice) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.player = player;
        this.startPrice = startPrice;
    }

    /**
     * Returns the players ID
     *
     * @return The players who this inventory is managing
     */
    public UUID getPlayerId() {
        return player;
    }

    /**
     * Returns the defined auction start price
     *
     * @return The auction start price
     */
    public double getStartPrice() {
        return startPrice;
    }

    /**
     * Opens the inventory to a player
     *
     * @param player The player to open the inventory
     */
    public void open(Player player) {
        if (!getInventory().getViewers().contains(player)) {
            player.openInventory(getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory == null ? inventory = Bukkit.createInventory(this, 6 * 9, title) : inventory;
    }
}

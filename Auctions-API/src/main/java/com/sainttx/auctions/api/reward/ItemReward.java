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

package com.sainttx.auctions.api.reward;

import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.Message;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * An implementation of {@link Reward} that gives players an item
 */
public class ItemReward implements Reward {

    private AuctionPlugin plugin;
    private ItemStack item;
    private Message notEnoughRoom = new Message() { // TODO: Default for isSpammy
        @Override
        public String getMessage() {
            return "messages.notEnoughRoom";
        }

        @Override
        public boolean isSpammy() {
            return false;
        }

        @Override
        public boolean isIgnorable() {
            return false;
        }
    };

    /* for deserialization of item rewards */
    public ItemReward(Map<String, Object> itemSerialized) {
        this.item = ItemStack.deserialize(itemSerialized);
    }

    public ItemReward(AuctionPlugin plugin, ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }

        this.plugin = plugin;
        this.item = item;
    }

    /**
     * Gets the item in this reward
     *
     * @return the item wrapped in this reward
     */
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void giveItem(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack reward = item.clone();

        Map<Integer, ItemStack> overflow = inventory.addItem(reward);
        if (!overflow.isEmpty()) {
            overflow.values().forEach((item) -> player.getWorld().dropItem(player.getLocation(), item));
            if (plugin != null) {
                plugin.getMessageFactory().submit(player, notEnoughRoom);
            }
        }
    }

    @Override
    public String getName() {
        return plugin.getItemName(item);
    }

    @Override
    public int getAmount() {
        return item.getAmount();
    }

    @Override
    public Map<String, Object> serialize() {
        return item.serialize();
    }
}

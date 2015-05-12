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

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.util.AuctionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * An implementation of {@link Reward} that gives players an item
 */
public class ItemReward implements Reward {

    private ItemStack item;

    /* for deserialization of item rewards */
    public ItemReward(Map<String, Object> itemSerialized) {
        this(ItemStack.deserialize(itemSerialized));
    }

    public ItemReward(ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }

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

        int free = AuctionUtil.getFreeSlots(inventory, reward);

        if (free < reward.getAmount()) { /* not enough space in inventory */
            ItemStack drop = reward.clone();
            drop.setAmount(drop.getAmount() - free);

            if (free > 0) {
                reward.setAmount(free);
                giveItemToPlayer(player, reward);
            }
            player.getWorld().dropItem(player.getLocation(), drop);
            AuctionsAPI.getMessageHandler().sendMessage(player, AuctionPlugin.getPlugin().getMessage("messages.notEnoughRoom"));
        } else {
            giveItemToPlayer(player, reward);
        }
    }

    /*
     * A helper method that gives an item to a player, taking
     * into consideration stack sizes
     */
    private void giveItemToPlayer(Player player, ItemStack item) {
        if (item.getAmount() > item.getMaxStackSize()) {
            while (item.getAmount() > item.getMaxStackSize()) {
                ItemStack give = item.clone();
                give.setAmount(item.getMaxStackSize());
                player.getInventory().addItem(give);

                // Lower the stack size
                item.setAmount(item.getAmount() - item.getMaxStackSize());
            }

            player.getInventory().addItem(item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    @Override
    public String getName() {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
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

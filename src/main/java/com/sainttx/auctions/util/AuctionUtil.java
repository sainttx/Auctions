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

package com.sainttx.auctions.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A utility class that handles various item and inventory functions
 */
public class AuctionUtil {

    /**
     * Gets the amount of slots available for a particular item
     *
     * @param inv the inventory to search
     * @param base the item to find slots for
     * @return the amount of items that {@link Inventory#addItem(ItemStack...)}
     * will be able to successfully put into the inventory
     */
    public static int getFreeSlots(Inventory inv, ItemStack base) {
        if (inv == null) {
            throw new IllegalArgumentException("inventory cannot be null");
        } else if (base == null) {
            throw new IllegalArgumentException("base item cannot be null");
        }

        int totalFree = 0;
        for (ItemStack is : inv.getContents()) {
            if (is == null || is.getType() == Material.AIR) {
                totalFree += base.getMaxStackSize();
            } else if (is.isSimilar(base)) {
                totalFree += is.getAmount() > base.getMaxStackSize() ? 0 : base.getMaxStackSize() - is.getAmount();
            }
        }
        return totalFree;
    }

    /**
     * Gets the amount of a specific item inside an inventory
     *
     * @param inv the inventory to search
     * @param base the item to search for
     * @return the amount of items that match {@link ItemStack#isSimilar(ItemStack)}
     * with the base
     */
    public static int getAmountItems(Inventory inv, ItemStack base) {
        if (inv == null) {
            throw new IllegalArgumentException("inventory cannot be null");
        } else if (base == null) {
            throw new IllegalArgumentException("base item cannot be null");
        }

        int count = 0;
        for (int i = 0 ; i < 36 ; i++) {
            ItemStack is = inv.getItem(i);
            if (is != null) {
                if (is.isSimilar(base)) {
                    count += is.getAmount();
                }
            }
        }

        return count;
    }
}

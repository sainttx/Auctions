package com.sainttx.auction.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AuctionUtil {

    /**
     * Gets the amount of slots available for a particular item
     *
     * @param inv  the inventory to search
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
     * @param inv  the inventory to search
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
        for (ItemStack is : inv) {
            if (is != null) {
                if (is.isSimilar(base)) {
                    count += is.getAmount();
                }
            }
        }

        return count;
    }
}

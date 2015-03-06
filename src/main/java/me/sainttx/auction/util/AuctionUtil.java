package me.sainttx.auction.util;

import me.sainttx.auction.AuctionPlugin;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AuctionUtil {
    
    /**
     * Gives an item to a player
     * 
     * @param player        The player to receive the item
     * @param itemstack     The item to be received
     * @param messages  Any messages to be sent to the player
     */
    public static void giveItem(Player player, ItemStack itemstack, String... messages) {
        World world = player.getWorld();
        boolean dropped = false;
        int maxsize     = itemstack.getMaxStackSize();
        int amount      = itemstack.getAmount();
        int stacks      = amount / maxsize;
        int remaining   = amount % maxsize;
        ItemStack[] split = new ItemStack[1];
        
        if (amount > maxsize) {
            split = new ItemStack[stacks + (remaining > 0 ? 1 : 0)];
            // ie. 70 stack can only be 64
            for (int i = 0 ; i < stacks ; i++) {
                ItemStack maxStackSize = itemstack.clone();
                maxStackSize.setAmount(maxsize);
                split[i] = maxStackSize;
            }
            if (remaining > 0) {
                ItemStack remainder = itemstack.clone();
                remainder.setAmount(remaining);
                split[stacks] = remainder;
            }
        } else {
            split[0] = itemstack;
        }

        for (ItemStack item : split) {            
            if (item != null) {
                // Check their inventory space
                if (hasSpace(player.getInventory(), item)) {
                    player.getInventory().addItem(item);
                } else {
                    world.dropItem(player.getLocation(), item);
                    dropped = true;
                }
            }
        }
        if (messages.length == 1) {
            TextUtil.sendMessage(TextUtil.getConfigMessage(messages[0]), true, player);
        } 
        if (dropped) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("items-no-space"), true, player);
        } 
    }

    /**
     * Checks if an inventory can fit a split itemstack
     * 
     * @param inventory The inventory to check
     * @param itemstack The item being put into the inventory
     * 
     * @return True if the inventory can fit the item, false otherwise
     */
    public static boolean hasSpace(Inventory inventory, ItemStack itemstack) {
        int totalFree = 0;
        for (ItemStack is : inventory.getContents()) {
            if (is == null) {
                totalFree += itemstack.getMaxStackSize();
            } else if (is.isSimilar(itemstack)) {
                totalFree += is.getAmount() > itemstack.getMaxStackSize() ? 0 : itemstack.getMaxStackSize() - is.getAmount();
            }
        }
        return totalFree >= itemstack.getAmount();
    }
    
    /**
     * Returns if an inventory has enough of an item
     * 
     * @param inv       The inventory to check
     * @param item      The item to find
     * @param numItems  The number of items searching for
     * 
     * @return True if the inventory has enough of the item, false otherwise  
     */
    public static boolean searchInventory(Inventory inv, ItemStack item, int numItems) {
        int count = 0;
        for (ItemStack is : inv) {
            if (is != null) { 
                if (is.isSimilar(item)) {
                    if (is.getAmount() >= numItems) {
                        return true;
                    } else {
                        count += is.getAmount();
                    }
                }
            }
        }
        if (count >= numItems) { 
            return true;
        }
        return false;
    }
    
    /**
     * Return a String representation of time left
     * 
     * @param timeLeft Time left in seconds
     * 
     * @return String the time left
     */
    public static String getFormattedTime(int timeLeft) {
        return TimeUtil.formatDateDiff(System.currentTimeMillis() + (timeLeft * 1000L),
                AuctionPlugin.getPlugin().getConfig().getBoolean("settings.use-shortened-times", false));
    }
}

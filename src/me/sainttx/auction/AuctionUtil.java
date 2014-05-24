package me.sainttx.auction;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AuctionUtil {
    
    private Messages messager;
    
    public AuctionUtil() {        
        messager = Messages.getMessager();
    }
    
    public void giveItem(Player player, ItemStack itemstack, String... messageentry) {
        World world = player.getWorld();
        boolean dropped = false;
        int maxsize = itemstack.getMaxStackSize();
        int amount = itemstack.getAmount();
        int stacks = amount / maxsize;
        int remaining = amount % maxsize;
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
        if (messageentry.length == 1) {
            messager.sendText((CommandSender) player, messageentry[0], true);
        } 
        if (dropped) {
            messager.sendText((CommandSender) player, "items-no-space", true);
        } 
    }

    private boolean hasSpace(Inventory inventory, ItemStack itemstack) {
        int total = 0;
        for (ItemStack is : inventory.getContents()) {
            if (is == null) {
                total += itemstack.getMaxStackSize();
            } else if (is.isSimilar(itemstack)) {
                total += itemstack.getMaxStackSize() - is.getAmount();
            }
        }
        return total >= itemstack.getAmount();
    }
    
    public boolean searchInventory(Player player, ItemStack item, int numItems) {
        int count = 0;
        for (ItemStack is : player.getInventory()) {
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
    
    public String getFormattedTime(int timeLeft) {     
        String formatted = "";
        int days = (int) Math.floor(timeLeft / 86400); // get days
        int hourSeconds = timeLeft % 86400; 
        int hours = (int) Math.floor(hourSeconds / 3600); // get hours
        int minuteSeconds = hourSeconds % 3600;
        int minutes = (int) Math.floor(minuteSeconds / 60); // get minutes
        int remainingSeconds = minuteSeconds % 60;
        int seconds = (int) Math.ceil(remainingSeconds); // get seconds

        if (days > 0) formatted += String.format("%d d, ", days);
        if (hours > 0) formatted += String.format("%d hr, ", hours);
        if (minutes > 0) formatted += String.format("%d min, ", minutes);
        if (seconds > 0) formatted += String.format("%d sec", seconds);

        return formatted;
    }
}

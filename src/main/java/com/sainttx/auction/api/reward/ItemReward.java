package com.sainttx.auction.api.reward;

import com.sainttx.auction.util.AuctionUtil;
import com.sainttx.auction.util.TextUtil;
import org.bukkit.entity.Player;
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
        AuctionUtil.giveItem(player, item);
    }

    @Override
    public String getName() {
        return TextUtil.getItemName(item);
    }

    @Override
    public Map<String, Object> serialize() {
        return item.serialize();
    }
}

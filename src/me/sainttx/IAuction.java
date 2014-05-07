package me.sainttx;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IAuction {
	private String owner;
	private int startingAmount;
	private int numItems;
	private ItemStack item;

	public IAuction(Player player, int numItems, int startingAmount) {
		this.startingAmount = startingAmount;
		this.numItems = numItems;
		owner = player.getName();
		item = player.getItemInHand();
		item.setAmount(1);
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getStartingAmount() {
		return startingAmount;
	}

	public void setStartingAmount(int startingAmount) {
		this.startingAmount = startingAmount;
	}

	public int getNumItems() {
		return numItems;
	}

	public void setNumItems(int numItems) {
		this.numItems = numItems;
	}

	public ItemStack getItem() {
		return item;
	}

	public void setItem(ItemStack item) {
		this.item = item;
	}
}

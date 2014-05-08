package me.sainttx;

import java.util.UUID;

import net.minecraft.server.v1_7_R3.Material;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class IAuction {
	private Auction plugin;
	private UUID owner;
	private int startingAmount;
	private int numItems;
	private int autoWin;
	private int timeRemaining;
	private int taskID;
	private int timeLeft;
	private int increment;
	private ItemStack item;

	private UUID winning;
	private int winningAmt;

	private final int[] times = {45, 30, 10, 3, 2, 1};

	public IAuction(Auction plugin, Player player, int numItems, int startingAmount, int autoWin) throws InsufficientItemsException {
		this.plugin = plugin;
		this.startingAmount = startingAmount;
		this.numItems = numItems;
		this.autoWin = autoWin;
		owner = player.getUniqueId();
		item = player.getItemInHand().clone();
		increment = plugin.getConfig().getInt("minimum-bid-increment");

		timeLeft = Integer.parseInt(plugin.getConfig().getString("auction-time")); // could throw on invalid

		if (searchInventory(player)) { // Check if they have enough
			takeItems(player);
		} else {
			// Doesn't have enough of the item
			throw new InsufficientItemsException();
		}
	}

	public void start() {
		plugin.messageListening("auction-start");
		plugin.messageListening("auction-start-price");
		Runnable task = new Runnable() {
			@Override
			public void run() {
				if (timeLeft <= 0) {
					end();
				} else {
					--timeLeft;
					for (int i : times) {
						if (i == timeLeft) {
							plugin.messageListening("auction-timer");
						}
					}
				}
			}
		};
		taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 0L, 20L);
	}

	public void bid(Player player, int amount) {
		if (amount <= winningAmt + increment) {
			player.sendMessage(plugin.getMessageFormatted("fail-bid-too-low"));
		} else if (winning.equals(player.getName())) {
			player.sendMessage(plugin.getMessageFormatted("fail-bid-top-bidder"));
		} else if (owner.equals(player.getName())) {
			player.sendMessage(plugin.getMessageFormatted("fail-bid-your-auction"));
		} else {
			// bid here
			if (amount >= autoWin && autoWin != -1) {
				// They win
				// Take away money
				plugin.messageListening("auction-ended-autowin");
				winning = player.getUniqueId();
				end();
			}
			plugin.messageListening("bid-broadcast");
		}
	}

	public boolean end() {
		Bukkit.getScheduler().cancelTask(taskID);
		if (winning == null) {
			plugin.messageListening("auction-end-no-bidders");
			plugin.stopAuction();
			// Return items to owner
			return true;
		}	
		OfflinePlayer winner = Bukkit.getOfflinePlayer(winning);
		OfflinePlayer owner = Bukkit.getOfflinePlayer(this.owner);
		if (winner.isOnline()) {
			Player winner1 = (Player) winner;
			winner1.sendMessage(plugin.getMessageFormatted("auction-winner"));
			winner1.sendMessage(plugin.getMessageFormatted("auction-end-tax"));
			// Give the items to the winner
		} else {

		}

		if (owner.isOnline()) {

		} else {

		}

		plugin.stopAuction();
		return true;
	}

	private boolean searchInventory(Player player) {
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

	private void takeItems(Player player) {
		Inventory playerInventory = player.getInventory();
		this.item.setAmount(numItems);
		int count = 0;
		for (ItemStack is : player.getInventory()) { // .all()
			if (is.isSimilar(item)) {
				int size = is.getAmount();
				if (size > numItems - count) {
					is.setAmount(size - (numItems - count));
					count = numItems;
				} else if(size == numItems - count) {
					count += size;
					playerInventory.removeItem(new ItemStack(item));
				} else {
					count += size;
					playerInventory.removeItem(new ItemStack(item));
				}
			}
			if (count == numItems) {
				break;
			}
		}
	}

	public UUID getOwner() {
		return owner;
	}

	public void setOwner(UUID owner) {
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

	public int getTimeRemaining() {
		return timeLeft;
	}
}

class InsufficientItemsException extends Exception {
	
}

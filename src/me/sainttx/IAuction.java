package me.sainttx;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IAuction {
	private Auction plugin;
	private UUID owner;
	private int numItems; // amount of items being auctioned
	private int autoWin;
	private int taskID;
	private int timeLeft;
	private int increment;
	private ItemStack item;

	private UUID winning;
	private int topBid;

	private final int[] times = {45, 30, 10, 3, 2, 1};

	public IAuction(Auction plugin, Player player, int numItems, int startingAmount, int autoWin) throws InsufficientItemsException, EmptyHandException {
		this.plugin = plugin;
		topBid = startingAmount;

		this.numItems = numItems;
		this.autoWin = autoWin;
		owner = player.getUniqueId();
		item = player.getItemInHand().clone();
		item.setAmount(numItems);
		increment = plugin.getConfig().getInt("minimum-bid-increment");

		timeLeft = Integer.parseInt(plugin.getConfig().getString("auction-time")); // could throw on invalid

		if (item.getType() == Material.AIR) {
			throw new EmptyHandException();
		}
		if (searchInventory(player)) { // Check if they have enough
			player.getInventory().removeItem(item);
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
		if (amount < topBid + increment) {
			player.sendMessage(plugin.getMessageFormatted("fail-bid-too-low"));
			return;
		} else if (owner.equals(player.getUniqueId())) {
			player.sendMessage(plugin.getMessageFormatted("fail-bid-your-auction"));
		} else {
			if (winning != null) {
				if (winning.equals(player.getUniqueId())) {
					player.sendMessage(plugin.getMessageFormatted("fail-bid-top-bidder"));
					return;
				}
			} 
			// bid here
			if (amount >= autoWin && autoWin != -1) {
				// They win
				// Take away money
				plugin.messageListening("auction-ended-autowin");
				winning = player.getUniqueId();
				end();
			}
			winning = player.getUniqueId();
			topBid = amount;
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

	public UUID getOwner() {
		return owner;
	}

	public UUID getWinning() {
		return winning;
	}

	public int getCurrentBid() {
		return topBid;
	}

	public int getNumItems() {
		return numItems;
	}

	public ItemStack getItem() {
		return item;
	}

	public int getTimeRemaining() {
		return timeLeft;
	}

	private final int secondsInAMinute = 60;
	private final int secondsInAnHour = 60 * secondsInAMinute;
	private final int secondsInADay = 24 * secondsInAnHour;

	public String getFormattedTime() {
		int time = timeLeft;
		String formatted = "";
		// Get days
		int days = (int) Math.floor(time / secondsInADay);

		// Get hours
		int hourSeconds = time % secondsInADay;
		int hours = (int) Math.floor(hourSeconds / secondsInAnHour);

		// Get minutes
		int minuteSeconds = hourSeconds % secondsInAnHour;
		int minutes = (int) Math.floor(minuteSeconds / secondsInAMinute);

		// Get seconds
		int remainingSeconds = minuteSeconds % secondsInAMinute;
		int seconds = (int) Math.ceil(remainingSeconds);

		if (days > 0) formatted += String.format("%d day(s), ", days);
		if (hours > 0) formatted += String.format("%d hour(s), ", hours);
		if (minutes > 0) formatted += String.format("%d minute(s), ", minutes);
		if (seconds > 0) formatted += String.format("%d second(s)", seconds);

		return formatted;
	}

	public int getCurrentTax() {
		int tax = plugin.getConfig().getInt("auction-tax-percentage");
		return topBid * (tax / 100);
	}

	public boolean hasBids() {
		return winning != null;
	}
}

@SuppressWarnings("serial")
class InsufficientItemsException extends Exception {

}

@SuppressWarnings("serial")
class EmptyHandException extends Exception {

}
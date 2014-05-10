package me.sainttx;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
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

	public IAuction(Auction plugin, Player player, int numItems, int startingAmount, int autoWin)
			throws InsufficientItemsException, EmptyHandException, UnsupportedItemException {
		this.plugin = plugin;
		this.numItems = numItems;
		topBid = startingAmount;
		owner = player.getUniqueId();
		item = player.getItemInHand().clone();
		item.setAmount(numItems);
		increment = plugin.getConfig().getInt("minimum-bid-increment");
		if ((autoWin < topBid + increment) && autoWin != -1) {
			this.autoWin = topBid + increment;
		} else {
			this.autoWin = autoWin;
		}
		try {
			timeLeft = Integer.parseInt(plugin.getConfig().getString("auction-time")); // could throw on invalid
		} catch (NumberFormatException ex1) {
			plugin.getLogger().severe("Config value auction-time is an invalid Integer");
		}

		if (item.getType() == Material.AIR) {
			throw new EmptyHandException();
		} 
		System.out.print(item.getType());
		if (item.getType() == Material.FIREWORK || item.getType() == Material.FIREWORK_CHARGE) {
			throw new UnsupportedItemException();
		}
		if (searchInventory(player)) { // Checks if they have enough of the item
			player.getInventory().removeItem(item);
		} else {
			throw new InsufficientItemsException();
		}
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

	public int getAutoWin() {
		return autoWin;
	}

	public int getCurrentTax() {
		int tax = plugin.getConfig().getInt("auction-tax-percentage");
		return topBid * (tax / 100);
	}

	public boolean hasBids() {
		return winning != null;
	}

	public String getTime() {
		return getFormattedTime();
	}

	public void start() {
		plugin.messageListening(plugin.getMessageFormatted("auction-start"));
		plugin.messageListening(plugin.getMessageFormatted("auction-start-price"));
		if (autoWin != -1) {
			plugin.messageListening(plugin.getMessageFormatted("auction-start-autowin"));
		}
		Runnable task = new Runnable() {
			@Override
			public void run() {
				if (timeLeft <= 0) {
					end();
				} else {
					--timeLeft;
					for (int i : times) {
						if (i == timeLeft) {
							plugin.messageListening(plugin.getMessageFormatted("auction-timer"));
							break;
						}
					}
				}
			}
		};
		taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 0L, 20L);
	}

	public void bid(Player player, int amount) {
		if (owner.equals(player.getUniqueId())) {
			plugin.getMessageFormatted("fail-bid-your-auction").send(player);
		} else if (amount < topBid + increment) {
			plugin.getMessageFormatted("fail-bid-too-low").send(player);
			return;
		} else {
			if (winning != null) {
				if (winning.equals(player.getUniqueId())) {
					plugin.getMessageFormatted("fail-bid-top-bidder").send(player);
					return;
				}
			}
			if (amount >= autoWin && autoWin != -1) {
				plugin.messageListening(plugin.getMessageFormatted("auction-ended-autowin"));
				winning = player.getUniqueId();
				end();
			}
			if (winning != null) {
				OfflinePlayer old = Bukkit.getOfflinePlayer(winning);
				Auction.getEconomy().depositPlayer(old.getName(), topBid);
			}
			winning = player.getUniqueId();
			topBid = amount;
			Auction.getEconomy().withdrawPlayer(player.getName(), topBid);
			plugin.messageListening(plugin.getMessageFormatted("bid-broadcast"));
		}
	}

	public boolean end() {
		Bukkit.getScheduler().cancelTask(taskID);
		OfflinePlayer owner = Bukkit.getOfflinePlayer(this.owner);
		if (winning == null) {
			plugin.messageListening(plugin.getMessageFormatted("auction-end-no-bidders"));
			plugin.stopAuction();
			// Return items to owner
			if (!owner.isOnline()) {
				System.out.print("Saving items of " + owner.getName());
				plugin.save(this.owner, item);
			} else {
				// return items to owner
				Player player = (Player) owner;
				plugin.giveItem(player, item, "nobidder-return");
			}
			return true;
		}
		OfflinePlayer winner = Bukkit.getOfflinePlayer(winning);
		if (winner.isOnline()) {
			Player winner1 = (Player) winner;
			plugin.giveItem(winner1, item, "winner-item");
			plugin.getMessageFormatted("auction-winner").send(winner1);
		} else {
			// Save the items
			YamlConfiguration logoff = plugin.getLogOff();
			if (logoff.getString(winner.getUniqueId().toString()) != null) {

			} else {
				plugin.save(winning, item); // TODO: check this out
			}
		}
		Auction.getEconomy().depositPlayer(owner.getName(), topBid - getCurrentTax());
		if (owner.isOnline()) {
			Player player = (Player) owner;
			plugin.getMessageFormatted("auction-ended").send(player);
			plugin.getMessageFormatted("auction-end-tax").send(player);
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

	private String getFormattedTime() {		
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

	@SuppressWarnings("serial")
	public class InsufficientItemsException extends Exception {

	}

	@SuppressWarnings("serial")
	public class EmptyHandException extends Exception {

	}

	@SuppressWarnings("serial")
	public class UnsupportedItemException extends Exception {

	}
}


package me.sainttx;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import mkremins.fanciful.FancyMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Auction extends JavaPlugin {
	private ArrayList<String> ignoring = new  ArrayList<String>();
	private YamlConfiguration messages;
	//private YamlConfiguration log;
	private IAuction auction;

	//	private boolean logauctions;
	//	private boolean allowautowin;
	//	private boolean allowcancel;
	//	private boolean allowcreative;
	//	private int timebetween;
	//	private int bidincrease;
	//	private int auctiontime;
	//	private int cost;
	//	private int percent;

	/*
	 * commands:
	 * auction info - show info about current auction
	 * auction start - start an auction
	 * auction end - end an auction
	 * auction quiet - silence auction messages
	 */

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		loadConfig();
		getCommand("auction").setExecutor(this);
	}

	private void loadConfig() {
		//		logauctions = getConfig().getBoolean("log-auctions");
		//		allowautowin = getConfig().getBoolean("allow-autowin");
		//		allowcancel = getConfig().getBoolean("allow-cancel");
		//		allowcreative = getConfig().getBoolean("allow-creative");
		//		timebetween = getConfig().getInt("time-between-bidding");
		//		bidincrease = getConfig().getInt("minimum-bid-increment");
		//		auctiontime = getConfig().getInt("auction-time");
		//		cost = getConfig().getInt("auction-start-fee");
		//		percent = getConfig().getInt("auction-tax-percentage");
		File messages = new File(getDataFolder(), "messages.yml");
		if (!messages.exists()) {
			saveResource("messages.yml", false);
		}
		this.messages = YamlConfiguration.loadConfiguration(messages);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		String username = sender.getName();
		if (args.length == 0) {
			sendMenu(sender);
		} else {
			String arg1 = args[0];
			if (arg1.equals("start")) {
				if (ignoring.contains(username)) {
					sender.sendMessage(getMessageFormatted("fail-start-ignoring"));
					return false;
				} else {
					startAuction(sender, args); // auction start <amt> <min-bid> [autowin]
				}
			} else if (arg1.equals("end")) {
				if (this.auction == null) {
					sender.sendMessage(getMessageFormatted("fail-end-no-auction"));
					return false;
				} else {
					auction.end();
					stopAuction();
				}
			} else if (arg1.equals("info")) {

			} else if (arg1.equals("bid")) {
				if (auction != null) {
					if (args.length == 2) {
						auction.bid((Player) sender, Integer.parseInt(args[1])); // could throw
					} else {
						sender.sendMessage(getMessageFormatted("fail-bid-syntax"));
					}
				} else {
					sender.sendMessage(getMessageFormatted("fail-bid-no-auction"));
				}
			} else if (arg1.equals("quiet") || arg1.equals("ignore")) {
				if (!ignoring.contains(sender.getName())) {
					sender.sendMessage(getMessageFormatted("ignoring-on"));
					ignoring.add(sender.getName());
				} else {
					sender.sendMessage(getMessageFormatted("ignoring-off"));
					ignoring.remove(sender.getName());
				}
			} else if (arg1.equals("reload")) {
				if (!sender.hasPermission("auction.reload")) {
					sender.sendMessage(getMessageFormatted("insufficient-permissions"));
					return false;
				}
				reloadConfig();
				loadConfig();
			} else if (arg1.equals("test")) {
				Player player = (Player) sender;
				player.getItemInHand().setAmount(5);
			} else if (arg1.equals("test1")) {
				Player player = (Player) sender;
				player.getItemInHand().setType(Material.AIR);
			}
			else {
				// invalid arg
				sendMenu(sender);
			}
		}
		return false;
	}

	public void messageListening(String message) {
		FancyMessage message0 = new FancyMessage();
		
//		String message1 = messages.getString(message);
//		message1 = message1.replaceAll("%i", auction.getItem().getType().toString())
//				.replaceAll("%t", auction.getFormattedTime())
//				.replaceAll("%b", Integer.toString(auction.getCurrentBid()))
//				.replaceAll("%p", UUIDtoName(auction.getOwner()));
//		try {
//			message1 = message1.replaceAll("%T", Integer.toString(auction.getCurrentTax()))
//					.replaceAll("%w", UUIDtoName(auction.getWinning()));
//		} catch (IllegalArgumentException ex1) {
//			// UUID is null
//		}
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!ignoring.contains(player.getName())) {
				player.sendMessage(message0.then(ChatColor.GREEN + "Test").toJSONString());
				//player.sendMessage(format(message1));
			}
		}
	}

	public String UUIDtoName(UUID uuid) {
		return Bukkit.getOfflinePlayer(uuid).getName();
	}

	public YamlConfiguration getMessages() {
		return this.messages;
	}

	private void sendMenu(CommandSender sender) {
		for (Iterator<String> info = messages.getStringList("auction-menu").iterator(); info.hasNext();) {
			sender.sendMessage(format(info.next()));
		}
	}

	private void startAuction(CommandSender sender, String[] args) {
		if (this.auction != null) {
			sender.sendMessage(getMessageFormatted("fail-start-auction-in-progress"));
			return;
		}
		if (sender instanceof Player) {
			if (args.length > 2) {
				try {
					int amount = Integer.parseInt(args[1]);
					int start = Integer.parseInt(args[2]);
					if (args.length == 4) { // auction start amount startingbid autowin
						int autowin = Integer.parseInt(args[3]);
						this.auction = new IAuction(this, (Player) sender, amount, start, autowin);
						this.auction.start();
						return;
					}
					this.auction = new IAuction(this, (Player) sender, amount, start, -1);
					this.auction.start();
				} catch (NumberFormatException ex1) {
					sender.sendMessage(getMessageFormatted("fail-number-format"));
				} catch (InsufficientItemsException ex2) {
					sender.sendMessage(getMessageFormatted("fail-start-not-enough-items"));	
				}
			} else {
				sender.sendMessage(getMessageFormatted("fail-start-syntax"));
			}
		} else {
			sender.sendMessage(messages.getString("fail-console"));
		}
	}

	public void stopAuction() {
		this.auction = null;
	}

	public String getMessageFormatted(String message) {
		return format(messages.getString(message));
	}

	private String format(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}
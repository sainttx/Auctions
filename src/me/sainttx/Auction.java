package me.sainttx;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Auction extends JavaPlugin {
	private ArrayList<String> ignoring = new  ArrayList<String>();
	private YamlConfiguration messages;
	private YamlConfiguration log;
	private IAuction auction;
	
	private boolean logauctions;
	private boolean allowautowin;
	private boolean allowcancel;
	private boolean allowcreative;
	private int timebetween;
	private int bidincrease;
	private int mintime;
	private int maxtime;
	private int cost;
	private int percent;
	
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
		logauctions = getConfig().getBoolean("log-auctions");
		allowautowin = getConfig().getBoolean("allow-autowin");
		allowcancel = getConfig().getBoolean("allow-cancel");
		allowcreative = getConfig().getBoolean("allow-creative");
		timebetween = getConfig().getInt("time-between-bidding");
		bidincrease = getConfig().getInt("minimum-bid-increment");
		mintime = getConfig().getInt("minimum-auction-time");
		maxtime = getConfig().getInt("maximum-auction-time");
		cost = getConfig().getInt("auction-start-fee");
		percent = getConfig().getInt("auction-tax-percentage");
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
			for (Iterator<String> info = messages.getStringList("auction-menu").iterator(); info.hasNext();) {
				sender.sendMessage(format(info.next()));
			}
		} else {
			String arg1 = args[0];
			if (arg1.equals("start")) {
				if (ignoring.contains(username)) {
					sender.sendMessage(format(messages.getString("fail-start-ignoring")));
					return false;
				} else {
					
				}
			} else if (arg1.equals("end")) {
				
			} else if (arg1.equals("info")) {
				
			} else if (arg1.equals("quiet") || arg1.equals("ignore")) {
				if (!ignoring.contains(sender.getName())) {
					sender.sendMessage(format(messages.getString("ignoring-on")));
					ignoring.add(sender.getName());
				} else {
					sender.sendMessage(format(messages.getString("ignoring-off")));
					ignoring.remove(sender.getName());
				}
			} else if (arg1.equals("reload")) {
				if (!sender.hasPermission("auction.reload")) {
					sender.sendMessage(format(messages.getString("insufficient-permissions")));
					return false;
				}
				reloadConfig();
				loadConfig();
			}
				
		}
		return false;
	}
	
	private String format(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}
package me.sainttx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import me.sainttx.IAuction.EmptyHandException;
import me.sainttx.IAuction.InsufficientItemsException;
import mkremins.fanciful.FancyMessage;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Auction extends JavaPlugin implements Listener {
	private IAuction auction;
	public static Economy economy = null;

	private ArrayList<String> ignoring = new  ArrayList<String>();
	private static HashMap<UUID, ItemStack> loggedoff = new HashMap<UUID, ItemStack>();

	private File off = new File(getDataFolder(), "save.yml");
	private YamlConfiguration messages;
	private YamlConfiguration names;
	private YamlConfiguration logoff;
	private File log;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		loadConfig();
		setupEconomy();
		loadSaved();
		getCommand("auction").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		if (auction != null) {
			auction.end();
		}
		try {
			if (!off.exists()) {
				off.createNewFile();
			}
			logoff.save(off);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void log(String s) {
		try {
			log.setWritable(true);
			BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), true));
			out.append(s + "\n");
			out.close();
		} catch (IOException e) {
		}
	}

	public static Economy getEconomy() {
		return economy;
	}

	public String UUIDtoName(UUID uuid) {
		return Bukkit.getOfflinePlayer(uuid).getName();
	}

	public YamlConfiguration getMessages() {
		return messages;
	}

	public YamlConfiguration getLogOff() {
		return logoff;
	}

	public void stopAuction() {
		auction = null;
	}

	public void save(UUID uuid, ItemStack is) { 
		logoff.set(uuid.toString(), is);
		loggedoff.put(uuid, is);
	}

	public void loadSaved() {
		for (String string : logoff.getStringList("")) {
			ItemStack is = logoff.getItemStack(string);
			UUID uuid = UUID.fromString(string);
			loggedoff.put(uuid, is);
		}
	}

	public FancyMessage getMessageFormatted(String path) {
		if (getConfig().getBoolean("log-auctions")) {
			String message0 = replace(messages.getString(path));
			if (message0.contains("%i")) {
				message0.replaceAll("%i", auction.getItem().getType().toString());
			}
			log(message0);
		}
		return format(messages.getString(path));
	}

	public void messageListening(FancyMessage message) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!ignoring.contains(player.getName())) {
				message.send(player);
			}
		}
	}

	private void loadConfig() {
		File messages = new File(getDataFolder(), "messages.yml");
		File names = new File(getDataFolder(), "items.yml");
		log = new File(getDataFolder(), "log.txt");
		if (!messages.exists()) {
			saveResource("messages.yml", false);
		}
		if (!names.exists()) {
			saveResource("items.yml", false);
		}
		try {
			if (!log.exists()) {
				log.createNewFile();
			}
			if (!off.exists()) {
				off.createNewFile();
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.messages = YamlConfiguration.loadConfiguration(messages);
		this.names = YamlConfiguration.loadConfiguration(names);
		this.logoff = YamlConfiguration.loadConfiguration(off);
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		ItemStack saved = logoff.getItemStack(player.getUniqueId().toString());
		if (saved != null) {
			giveItem(player, saved);
			getMessageFormatted("saved-item-return").send(player);
			logoff.set(player.getUniqueId().toString(), null);
		}
	}

	public void giveItem(Player player, ItemStack itemstack) {
		ItemStack[] itemstacksplit = splitStack(itemstack);
		World world = player.getWorld();
		boolean dropped = false;
		for (ItemStack item : itemstacksplit) {
			if (item != null) {
				// Check their inventory space
				if (hasSpace(player.getInventory(), itemstack)) {
					player.getInventory().addItem(item);
				} else {
					world.dropItem(player.getLocation(), item);
					dropped = true;
				}
			}
		}
		if (dropped) {
			getMessageFormatted("items-no-space").send(player);
		} else {
			getMessageFormatted("success-items-recieved").send(player); // TODO: figure out which message to send
		}
	}

	private ItemStack[] splitStack(ItemStack itemstack) {
		ItemStack copy = itemstack.clone();
		int maxsize = copy.getMaxStackSize();
		int amount = copy.getAmount();
		int arraysize = (int) Math.ceil(amount / maxsize);

		ItemStack[] itemstackarray = new ItemStack[arraysize == 0 ? 1 : arraysize];
		if (amount > maxsize) {
			for (int i = 0 ; i < itemstackarray.length ; i++) {
				/*
				 *  Goes through each slot in the array..
				 *  Need to split every item stack to the max size unless its less
				 */
				if (amount <= maxsize) {
					// last item stack = amount
					copy.setAmount(amount);
					itemstackarray[i] = copy.clone();
					break;
				}
				copy.setAmount(maxsize);
				itemstackarray[i] = copy.clone();
			}
		} else {
			itemstackarray[0] = copy;
		}
		return itemstackarray;
	}

	// This works
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

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		String username = sender.getName();
		if (args.length == 0) {
			sendMenu(sender);
		} else {
			if (sender instanceof ConsoleCommandSender) {
				System.out.print("Auction commands are currently disabled from the console.");
				return false;
			}
			String arg1 = args[0];
			Player player = (Player) sender;
			if (arg1.equals("start")) {
				if (!ignoring.contains(username)) {
					startAuction(player, args);
				} else {
					getMessageFormatted("fail-start-ignoring").send(player);
				}
			} else if (arg1.equals("end")) {
				if (this.auction != null) {
					auction.end();
					stopAuction();
				} else {
					getMessageFormatted("fail-end-no-auction").send(player);
				}
			} else if (arg1.equals("info")) {
				if (auction != null) {
					getMessageFormatted("auction-info-message").send(player);
				} else {
					getMessageFormatted("fail-info-no-auction").send(player);
				}
			} else if (arg1.equals("bid")) {
				if (auction != null) {
					if (args.length == 2) {
						auction.bid(player, Integer.parseInt(args[1])); // could throw
					} else {
						getMessageFormatted("fail-bid-syntax").send(player);
					}
				} else {
					getMessageFormatted("fail-bid-no-auction").send(player);
				}
			} else if (arg1.equals("quiet") || arg1.equals("ignore")) {
				if (!ignoring.contains(sender.getName())) {
					getMessageFormatted("ignoring-on").send(player);
					ignoring.add(sender.getName());
				} else {
					getMessageFormatted("ignoring-off").send(player);
					ignoring.remove(sender.getName());
				}
			} else if (arg1.equals("reload")) {
				if (sender.hasPermission("auction.reload")) {
					reloadConfig();
					loadConfig();	
				} else {
					getMessageFormatted("insufficient-permissions").send(player);
				}
			} else if (arg1.equals("stop")) {
				Bukkit.getPluginManager().disablePlugin(this);
			} else if (arg1.equals("test")) {

			}
			else {
				sendMenu(sender); // invalid arg
			}
		}
		return false;
	}

	private void startAuction(Player player, String[] args) {
		if (this.auction != null) {
			getMessageFormatted("fail-start-auction-in-progress").send(player);
			return;
		}
		if (args.length > 2) {
			try {
				int amount = Integer.parseInt(args[1]);
				int start = Integer.parseInt(args[2]);
				int autowin = -1;
				if (args.length == 4) { // auction start amount startingbid autowin
					autowin = Integer.parseInt(args[3]);
				}
				this.auction = new IAuction(this, player, amount, start, autowin);
				this.auction.start();
			} catch (NumberFormatException ex1) {
				getMessageFormatted("fail-number-format").send(player);
			} catch (InsufficientItemsException ex2) {
				getMessageFormatted("fail-start-not-enough-items").send(player);	
			} catch (EmptyHandException ex3) {
				getMessageFormatted("fail-start-handempty").send(player);	
			}
		} else {
			getMessageFormatted("fail-start-syntax").send(player);
		}
	} 


	private String itemName(ItemStack item) {
		short durability = item.getType().getMaxDurability() > 0 ? 0 : item.getDurability();
		String search = item.getType().toString() + "." + durability;
		String ret = names.getString(search);
		if (ret == null) {
			ret = "null";
		}
		return ret;
	}

	private ChatColor getIColor(String type) {
		return ChatColor.getByChar(messages.getString("%i." + type));		
	}

	private String replace(String message) {
		String ret = message;
		if (auction != null) {
			ret = ret.replaceAll("%t", auction.getTime())
					.replaceAll("%b", Integer.toString(auction.getCurrentBid()))
					.replaceAll("%p", UUIDtoName(auction.getOwner()))
					.replaceAll("%a", Integer.toString(auction.getNumItems()));
			if (auction.hasBids()) {
				ret = ret.replaceAll("%T", Integer.toString(auction.getCurrentTax()))
						.replaceAll("%w", UUIDtoName(auction.getWinning()));
			}
		}
		return ChatColor.translateAlternateColorCodes('&', ret);
	}

	private void sendMenu(CommandSender sender) {
		for (Iterator<String> info = messages.getStringList("auction-menu").iterator(); info.hasNext();) {
			sender.sendMessage(replace(info.next()));
		}
	}

	private FancyMessage format(String message) {
		FancyMessage message0 = new FancyMessage("");
		String message1 = replace(message);

		String[] split = message1.split("%i");
		if (message1.contains("%i")) {
			for (int i = 0 ; i < split.length ; i++) {
				if (i != split.length -1) {
					message0.then(split[i])
					.then(itemName(auction.getItem()))
					.itemTooltip(auction.getItem());
				} else {
					if (message1.endsWith("%i")) {
						if (split.length == 1) message0.then(split[i]);
						message0.then(itemName(auction.getItem()))
						.itemTooltip(auction.getItem());
					} else {
						message0.then(split[i]);
					}
				}
				message0.color(getIColor("color"));
				if (!messages.getString("%i.style").equals("none")) {
					message0.style(getIColor("style"));
				}
			}
		} else {
			message0.then(message1);
		}
		return message0;
	}
}
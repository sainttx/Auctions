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
import me.sainttx.IAuction.UnsupportedItemException;
import mkremins.fanciful.FancyMessage;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
	private static HashMap<String, ItemStack> loggedoff = new HashMap<String, ItemStack>();

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
		getCommand("bid").setExecutor(this);
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
			out.append(s.replaceAll(ChatColor.COLOR_CHAR + "[.]", "") + "\n");
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
		loggedoff.put(uuid.toString(), is);
		try {
			logoff.save(off);
		} catch (IOException e) {
		}
	}

	public void loadSaved() {
		for (String string : logoff.getKeys(false)) {
			ItemStack is = logoff.getItemStack(string);
			loggedoff.put(string, is);
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
		ItemStack saved = loggedoff.get(player.getUniqueId().toString());
		if (saved != null) {
			giveItem(player, saved, "saved-item-return");
			loggedoff.remove(player.getUniqueId().toString());
			logoff.set(player.getUniqueId().toString(), null);
			try {
				logoff.save(off);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void giveItem(Player player, ItemStack itemstack, String... messageentry) {
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
			if (messageentry.length == 1) {
				getMessageFormatted(messageentry[0]).send(player);	
			} else {
				getMessageFormatted("give-item-unkown").send(player);
			}
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
		if (args.length == 0 && !cmd.getLabel().toLowerCase().equals("bid")) {
			sendMenu(sender);
		} else {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (cmd.getLabel().toLowerCase().equals("bid")) {
					if (!player.hasPermission("auction.bid")) {
						getMessageFormatted("insufficient-permissions").send(player);
						return false;
					}
					if (args.length == 1) {
						try {
							if (auction != null) {
								auction.bid(player, Integer.parseInt(args[0]));
							}
							else {
								getMessageFormatted("fail-bid-no-auction").send(player);
							}
						} catch (NumberFormatException ex1) {
							getMessageFormatted("fail-bid-number").send(player);
						}
					} else {
						getMessageFormatted("fail-bid-syntax").send(player);
					}
					return true;
				}
				String arg1 = args[0].toLowerCase();
				if (!player.hasPermission("auction." + arg1)) {
					getMessageFormatted("insufficient-permissions").send(player);
					return false;
				}
				if (arg1.equals("start")) {
					if (!ignoring.contains(username)) {
						if (player.getGameMode() == GameMode.CREATIVE && !getConfig().getBoolean("allow-creative")) {
							getMessageFormatted("fail-start-creative").send(player);
							return false;
						}
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
							try{
								auction.bid(player, Integer.parseInt(args[1])); // could throw
							} catch (NumberFormatException ex1) {
								getMessageFormatted("fail-bid-number").send(player);
							}
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
				} else if (!arg1.equals("reload")) {
					sendMenu(player);
					return false;
				}
			}
			if (args[0].toLowerCase().equals("reload")) {
				if (sender.hasPermission("auction.reload")) {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.getString("reload")));
					reloadConfig();
					loadConfig();
				} else {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.getString("insufficient-permissions")));
				}
			} else {
				if (sender instanceof ConsoleCommandSender) {
					System.out.print("Console can't use this command.");
					return false;
				} 
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
				int fee = getConfig().getInt("auction-start-fee");
				if (fee > economy.getBalance(player.getName())) {
					getMessageFormatted("fail-start-no-funds").send(player);
					return;
				}
				economy.withdrawPlayer(player.getName(), fee);
				if (args.length == 4) { // auction start amount startingbid autowin
					if (getConfig().getBoolean("allow-autowin")) {
						autowin = Integer.parseInt(args[3]);
					} else {
						getMessageFormatted("fail-start-no-autowin").send(player);
						return;
					}
				}
				this.auction = new IAuction(this, player, amount, start, autowin);
				this.auction.start();
			} catch (NumberFormatException ex1) {
				getMessageFormatted("fail-number-format").send(player);
			} catch (InsufficientItemsException ex2) {
				getMessageFormatted("fail-start-not-enough-items").send(player);	
			} catch (EmptyHandException ex3) {
				getMessageFormatted("fail-start-handempty").send(player);	
			} catch (UnsupportedItemException ex4) {
				getMessageFormatted("unsupported-item").send(player);
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
					.replaceAll("%a", Integer.toString(auction.getNumItems()))
					.replaceAll("%A", Integer.toString(auction.getAutoWin()));
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
		//"&b%w &ahas bid &c$%b &aon the auction for %a %i"
		FancyMessage message0 = new FancyMessage("");
		String message1 = replace(message);

		if (message1.contains("%i")) {
			String[] split = message1.split("%i");
			if (split.length == 1) { // %i was only at the end
				message0.then(split[0]).then(itemName(auction.getItem())).itemTooltip(auction.getItem()).color(getIColor("color"));
				//message0.color(getIColor("color"));
				if (!messages.getString("%i.style").equals("none")) {
					message0.style(getIColor("style"));
				}
			} else {
				// more than 1 %i
				if (message1.endsWith("%i")) {
					for (int i = 0 ; i < split.length ; i++) {
						//[asdfoam, asdofka] %i
						message0.then(split[i]).then(itemName(auction.getItem())).itemTooltip(auction.getItem()).color(getIColor("color"));
						//message0.color(getIColor("color"));
						if (!messages.getString("%i.style").equals("none")) {
							message0.style(getIColor("style"));
						}
					}
				} else {
					for (int i = 0 ; i < split.length - 1 ; i++) {
						message0.then(split[i]).then(itemName(auction.getItem())).itemTooltip(auction.getItem()).color(getIColor("color"));
						//message0.color(getIColor("color"));
						if (!messages.getString("%i.style").equals("none")) {
							message0.style(getIColor("style"));
						}
					}
					message0.then(split[split.length - 1]);
				}
			}			
		} else {
			return message0.then(message1);
		}
		return message0;
	}
}
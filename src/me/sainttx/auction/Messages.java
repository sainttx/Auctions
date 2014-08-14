package me.sainttx.auction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import mkremins.fanciful.FancyMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Messages {

    private static Messages messages = null;
    private AuctionPlugin plugin;
    private YamlConfiguration messageFile;
    private YamlConfiguration names;
    private File log;

    private ArrayList<String> ignoring = new  ArrayList<String>();

    private Messages() {
        plugin = AuctionPlugin.getPlugin();
        loadFile();
    }

    public static Messages getMessager() {
        return messages == null ? messages = new Messages() : messages;
    }
    
    public YamlConfiguration getMessageFile() {
        return messageFile;
    }
    
    public void save() {
        try {
            File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");
            messageFile.save(messagesFile);
        } catch (IOException ex1) {
            
        }
    }

    private void loadFile() {
        File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");
        File namesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "items.yml");
        log = new File(AuctionPlugin.getPlugin().getDataFolder(), "log.txt");
        if (!messagesFile.exists()) {
            AuctionPlugin.getPlugin().saveResource("messages.yml", true);
        }
        if (!namesFile.exists()) {
            AuctionPlugin.getPlugin().saveResource("items.yml", false);
        }
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException ex1) {
                
            }
        }
        messageFile = YamlConfiguration.loadConfiguration(messagesFile);
        names = YamlConfiguration.loadConfiguration(namesFile);
    }

    public void reload() {
        loadFile();
    }

    public boolean isIgnoring(String name) {
        return ignoring.contains(name);
    }

    public void addIgnoring(String name) {
        ignoring.add(name);
    }

    public void removeIgnoring(String name) {
        ignoring.remove(name);
    }

    public void sendText(CommandSender sender, String text, boolean configEntry) {
        sender.sendMessage(color(configEntry ? getString(text) : text));
    }

    public void sendText(Player player, String text, boolean configentry) {
        sendText(player, text, configentry);
    }

    public void sendText(Player player, Auction auction, String text, boolean configEntry) {
        createFancyMessage(auction, configEntry ? getString(text) : text).send(player);
    }
    
    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public void messageListeningAll(Auction auction, String message, boolean configEntry) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ignoring.contains(player.getName())) {
                createFancyMessage(auction, configEntry ? getString(message) : message).send(player);
            }
        }
    }
    
    public FancyMessage createFancyMessage(Auction auction, String message) {
        FancyMessage fancyMessage = new FancyMessage(Messages.getMessager().color("&f"));
        String fancyText = replace(auction, message);
        ItemStack item = auction.getItem();

        if (fancyText.contains("%i")) {
            String[] split = fancyText.split("%i");
            if (split.length == 1) { // %i was only at the end
                fancyMessage.then(split[0]).then(getItemName(item)).itemTooltip(item).color(getIColor("color"));
                if (!getString("%i.style").equals("none")) {
                    fancyMessage.style(getIColor("style"));
                }
            } else {
                // more than 1 %i
                if (fancyText.endsWith("%i")) {
                    for (int i = 0 ; i < split.length ; i++) {
                        fancyMessage.then(split[i]).then(getItemName(item)).itemTooltip(item).color(getIColor("color"));
                        if (!getString("%i.style").equals("none")) {
                            fancyMessage.style(getIColor("style"));
                        }
                    }
                } else {
                    for (int i = 0 ; i < split.length - 1 ; i++) {
                        fancyMessage.then(split[i]).then(getItemName(item)).itemTooltip(item).color(getIColor("color"));
                        if (!getString("%i.style").equals("none")) {
                            fancyMessage.style(getIColor("style"));
                        }
                    }
                    fancyMessage.then(split[split.length - 1]);
                }
            }           
        } else {
            return fancyMessage.then(fancyText);
        }
        return fancyMessage;
    }

    private String getString(String path) {
        return messageFile.getString(path);
    }

    private String getItemName(ItemStack item) {
        short durability = item.getType().getMaxDurability() > 0 ? 0 : item.getDurability();
        String search = item.getType().toString() + "." + durability;
        String ret = names.getString(search);
        if (ret == null) {
            ret = "null";
        }
        return ret;
    }

    private ChatColor getIColor(String type) {
        return ChatColor.getByChar(getString("%i." + type));       
    }

    private String replace(Auction auction, String message) {
        String ret = message;
        if (auction != null) {
            ret = ret.replaceAll("%t", auction.getTime())
                    .replaceAll("%b", NumberFormat.getInstance().format(auction.getTopBid()))
                    .replaceAll("%p", UUIDtoName(auction.getOwner()))
                    .replaceAll("%a", Integer.toString(auction.getNumItems()))
                    .replaceAll("%A", NumberFormat.getInstance().format(auction.getAutoWin()));
            if (auction.hasBids()) {
                ret = ret.replaceAll("%T", Double.toString(auction.getCurrentTax()))
                        .replaceAll("%w", UUIDtoName(auction.getWinning()));
            }
        }

        if (plugin.isLogging()) {
            log(ret.replaceAll("%i", auction.getItem().getType().toString()));
        }

        return ChatColor.translateAlternateColorCodes('&', ret);
    }

    public void sendMenu(CommandSender sender) {
        for (Iterator<String> info = messageFile.getStringList("auction-menu").iterator(); info.hasNext();) {
            sender.sendMessage(color(info.next()));
        }
    }

    private String last = "";
    
    public void log(String s) {
        if (last.equals(s)) {
            return;
        }
        last = s;
        try {
            log.setWritable(true);
            BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), true));
            out.append(color(s).replaceAll("[" + ChatColor.COLOR_CHAR + "&][.]", "") + "\n");
            out.close();
        } catch (IOException e) {
        }
    }

    public String UUIDtoName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return Bukkit.getOfflinePlayer(uuid).getName();
        } else {
            return player.getName();
        }
    }
}

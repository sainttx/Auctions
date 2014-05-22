package me.sainttx.auction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import mkremins.fanciful.FancyMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Messages {

    private static Messages messages = null;
    private YamlConfiguration messageFile;
    private YamlConfiguration names;
    private File log;

    private ArrayList<String> ignoring = new  ArrayList<String>();

    private Messages() {
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
            File messagesFile = new File(Auction.getPlugin().getDataFolder(), "messages.yml");
            messageFile.save(messagesFile);
        } catch (IOException ex1) {
            
        }
    }

    private void loadFile() {
        File messagesFile = new File(Auction.getPlugin().getDataFolder(), "messages.yml");
        File namesFile = new File(Auction.getPlugin().getDataFolder(), "items.yml");
        log = new File(Auction.getPlugin().getDataFolder(), "log.txt");
        if (!messagesFile.exists()) {
            Auction.getPlugin().saveResource("messages.yml", true);
        }
        if (!namesFile.exists()) {
            Auction.getPlugin().saveResource("items.yml", false);
        }
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException ex1) {

            }
        }
        messageFile = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream messageStream = Auction.getPlugin().getResource("messages.yml");
        if (messageStream != null) {
            YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(messageStream);
            messageFile.setDefaults(defMessages);
        }
        
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

    public void sendText(CommandSender sender, String text, boolean configentry) {
        if (configentry) {
            sender.sendMessage(color(getString(text)));
        } else {
            sender.sendMessage(color(text));
        }
    }

    public void sendText(Player player, String text, boolean configentry) {
        sendText((CommandSender) player, text, configentry);
    }

    public void sendText(Player player, IAuction auction, String text, boolean configentry) {
        if (configentry) {
            getFancyMessage(auction, getString(text), false).send(player);
        } else {
            getFancyMessage(auction, text, configentry).send(player);
        }
    }
    
    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public void messageListeningAll(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ignoring.contains(player.getName())) {
                player.sendMessage(color(message));
            }
        }
    }

    public void messageListeningAll(IAuction auction, String message, boolean configentry, boolean world) {
        if (world && Auction.getConfiguration().getBoolean("per-world-auctions")) {
            messageListeningWorld(auction, message, configentry);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ignoring.contains(player.getName())) {
                getFancyMessage(auction, message, configentry).send(player);
            }
        }
    }

    public void messageListeningAllOther(IAuction auction, String message, boolean configentry) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(auction.getWorld()) && !ignoring.contains(player.getName())) {
                getFancyMessage(auction, message, configentry).send(player);
            }
        }
    }

    public void messageListeningWorld(IAuction auction, String message, boolean configentry) {
        World world = auction.getWorld();
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (!ignoring.contains(player.getName())) {
                getFancyMessage(auction, message, configentry).send(player);;
            }
        }
    }

    public FancyMessage getFancyMessage(IAuction auction, String text, boolean configentry) {
        String message = text;
        if (configentry) {
            message = getString(text);
        }
        return createFancyMessage(auction, message);
    }

    private FancyMessage createFancyMessage(IAuction auction, String message) {
        FancyMessage fancyMessage = new FancyMessage("");
        String fancyText = replace(auction, message);
        ItemStack item = auction.getItem();

        if (fancyText.contains("%i")) {
            String[] split = fancyText.split("%i");
            if (split.length == 1) { // %i was only at the end
                fancyMessage.then(split[0]).then(getItemName(item)).itemTooltip(item).color(getIColor("color"));
                //message0.color(getIColor("color"));
                if (!getString("%i.style").equals("none")) {
                    fancyMessage.style(getIColor("style"));
                }
            } else {
                // more than 1 %i
                if (fancyText.endsWith("%i")) {
                    for (int i = 0 ; i < split.length ; i++) {
                        fancyMessage.then(split[i]).then(getItemName(item)).itemTooltip(item).color(getIColor("color"));
                        //message0.color(getIColor("color"));
                        if (!getString("%i.style").equals("none")) {
                            fancyMessage.style(getIColor("style"));
                        }
                    }
                } else {
                    for (int i = 0 ; i < split.length - 1 ; i++) {
                        fancyMessage.then(split[i]).then(getItemName(item)).itemTooltip(item).color(getIColor("color"));
                        //message0.color(getIColor("color"));
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

    private String replace(IAuction auction, String message) {
        String ret = message;
        if (auction != null) {
            ret = ret.replaceAll("%t", auction.getTime())
                    .replaceAll("%b", Double.toString(auction.getTopBid()))
                    .replaceAll("%p", UUIDtoName(auction.getOwner()))
                    .replaceAll("%a", Integer.toString(auction.getNumItems()))
                    .replaceAll("%A", Double.toString(auction.getAutoWin()))
                    .replaceAll("%W", auction.getWorld().getName());
            if (auction.hasBids()) {
                ret = ret.replaceAll("%T", Double.toString(auction.getCurrentTax()))
                        .replaceAll("%w", UUIDtoName(auction.getWinning()));
            }
        }

        if (Auction.getConfiguration().getBoolean("log-auctions")) {
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

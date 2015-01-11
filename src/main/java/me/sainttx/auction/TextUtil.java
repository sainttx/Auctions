package me.sainttx.auction;

import mkremins.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class TextUtil {

    private static TextUtil textUtil = null;

    private YamlConfiguration messageFile;
    private YamlConfiguration names;

    private ArrayList<String> ignoring = new  ArrayList<String>();

    /**
     * Instantiates the messages manager
     */
    private TextUtil() {
        loadFiles();
    }

    /**
     * Returns the Messager instance, creates a new messager if it has
     * never been instantiated
     * 
     * @return Messages The Messages instance
     */
    
    public static TextUtil getMessager() {
        return textUtil == null ? textUtil = new TextUtil() : textUtil;
    }

    /**
     * Saves the messages configuration to file
     */
    public void save() {
        File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");            
        AuctionPlugin.getPlugin().saveFile(messageFile, messagesFile);
    }

    /* Load all message files */
    public void loadFiles() {
        File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");
        File namesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "items.yml");
        if (!messagesFile.exists()) {
            AuctionPlugin.getPlugin().saveResource("messages.yml", true);
        }
        if (!namesFile.exists()) {
            AuctionPlugin.getPlugin().saveResource("items.yml", false);
        }
        
        messageFile = YamlConfiguration.loadConfiguration(messagesFile);
        names = YamlConfiguration.loadConfiguration(namesFile);
    }

    /**
     * Returns if a player is ignoring auctions
     * 
     * @param name The name of the player ignoring
     *
     * @return True if the player is ignoring auctions, false otherwise
     */
    public boolean isIgnoring(String name) {
        return ignoring.contains(name);
    }

    /**
     * Adds a player to the ignoring list 
     * 
     * @param name The name of the player thats now ignoring
     */
    public void addIgnoring(String name) {
        ignoring.add(name);
    }

    /**
     * Removes a name from the ignoring list
     * 
     * @param name The name of the player not ignoring
     */
    public void removeIgnoring(String name) {
        ignoring.remove(name);
    }

    /**
     * Sends a message to a Conversable entity
     * 
     * @param sender        The conversable to send the message too
     * @param text          The message to send
     * @param configEntry   Whether or not the message provided was in the configuration
     */
    public void sendText(CommandSender sender, String text, boolean configEntry) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', (configEntry ? getString(text) : text)));
    }

    /**
     * Sends a FancyMessage to a Player
     * 
     * @param player        The player to send the message to
     * @param auction       The current auction
     * @param text          The text to be sent
     * @param configEntry   Whether or not the message provided was in the configuration
     */
    public void sendText(Player player, Auction auction, String text, boolean configEntry) {
        createFancyMessage(auction, configEntry ? getString(text) : text).send(player);
    }

    @SuppressWarnings("deprecation")
    /**
     * Messages all players information about the auction
     * 
     * @param auction       The current auction
     * @param message       The message to be sent
     * @param configEntry   Whether or not the message provided was in the configuration
     */
    public void messageListeningAll(Auction auction, String message, boolean configEntry) {
        FancyMessage msg = createFancyMessage(auction, configEntry ? getString(message) : message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ignoring.contains(player.getName())) {
                msg.send(player);
            }
        }
    }

    /**
     * Creates a FancyMessage about an auction
     * 
     * @param auction The current auction
     * @param message The message to replace information with
     * 
     * @return The FancyMessage created with text replaced by auction information 
     */
    public FancyMessage createFancyMessage(Auction auction, String message) {
        FancyMessage fancyMessage = new FancyMessage(ChatColor.WHITE.toString());
        String fancyText = replace(auction, message);
        ItemStack item = auction.getItem();

        if (fancyText.contains("%i")) {
            String[] split = fancyText.split(" ");
            ChatColor last = ChatColor.WHITE;
            for (String word : split) {
                word = ChatColor.translateAlternateColorCodes('&', word);
                String lastColors = ChatColor.getLastColors(word);
                last = ChatColor.getByChar(lastColors.isEmpty() || lastColors.equals("") ? last.getChar() : lastColors.charAt(1));
                
                if (word.equals("%i")) {
                    ChatColor color = getIColor("color");
                    fancyMessage.then(getItemName(item)).itemTooltip(item).color(color);
                    if (!getString("%i.style").equals("none")) {
                        fancyMessage.style(getIColor("style"));
                    }
                } else {
                    fancyMessage.then(word).color(last);
                }
                fancyMessage.then(" ");
            }   
        } else {
            return fancyMessage.then(fancyText);
        }
        return fancyMessage;
    }

    /* Gets a string from the messages file */
    private String getString(String path) {
        return messageFile.getString(path);
    }

    /* Gets an items name */
    private String getItemName(ItemStack item) {
        short durability = item.getType().getMaxDurability() > 0 ? 0 : item.getDurability();
        String search = item.getType().toString() + "." + durability;
        String ret = names.getString(search);

        return ret == null ? "null" : ret;
    }

    /* Gets the color for the item */
    private ChatColor getIColor(String type) {
        ChatColor c = ChatColor.getByChar(getString("%i." + type));
        
        return c == null ? ChatColor.WHITE : c;       
    }

    /* Replaces a String with Auction information */
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
        
        return ChatColor.translateAlternateColorCodes('&', ret);
    }

    /**
     * Sends the auction menu to a Conversable entity
     * 
     * @param sender The entity to send the menu too
     */
    public void sendMenu(CommandSender sender) {
        for (Iterator<String> info = messageFile.getStringList("auction-menu").iterator(); info.hasNext();) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', info.next()));
        }
    }

    /**
     * Converts a UUID to a name
     * 
     * @param uuid The unique ID of a player
     * 
     * @return String The name of the player with the UUID
     */
    public String UUIDtoName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return Bukkit.getOfflinePlayer(uuid).getName();
        } else {
            return player.getName();
        }
    }

    public YamlConfiguration getMessageFile() {
        return messageFile;
    }
}

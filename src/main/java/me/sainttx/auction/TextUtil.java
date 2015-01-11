package me.sainttx.auction;

import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TextUtil {

    /*
     * The file containing all the configurable messages in the plugin
     */
    private static YamlConfiguration messageFile;

    /*
     * The file containing item names
     */
    private static YamlConfiguration itemsFile;

    /*
     * A set containing the users that are ignoring this plugin
     */
    private static Set<UUID> ignoredUsers = new HashSet<UUID>();

    /**
     * Loads the messages file and names file from a plugin
     */
    public static void load(JavaPlugin plugin) {
        File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");
        File namesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "items.yml");
        if (!messagesFile.exists()) {
            AuctionPlugin.getPlugin().saveResource("messages.yml", true);
        }
        if (!namesFile.exists()) {
            AuctionPlugin.getPlugin().saveResource("items.yml", false);
        }

        messageFile = YamlConfiguration.loadConfiguration(messagesFile);
        itemsFile = YamlConfiguration.loadConfiguration(namesFile);
    }

    /**
     * Saves the messages configuration to file
     */
    public static void save() {
        File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");            
        AuctionPlugin.getPlugin().saveFile(messageFile, messagesFile);
    }

    /**
     * Returns if a player is ignoring auctions
     * 
     * @param uuid The ID of the player ignoring
     *
     * @return True if the player is ignoring auctions, false otherwise
     */
    public static boolean isIgnoring(UUID uuid) {
        return ignoredUsers.contains(uuid);
    }

    /**
     * Adds a player to the ignoring list 
     * 
     * @param uuid The ID of the player thats now ignoring
     */
    public static void addIgnoring(UUID uuid) {
        ignoredUsers.add(uuid);
    }

    /**
     * Removes a name from the ignoring list
     * 
     * @param uuid The uuid of the player not ignoring
     */
    public static void removeIgnoring(UUID uuid) {
        ignoredUsers.remove(uuid);
    }

    /**
     * Sends a FancyMessage to a player
     *
     * @param message The message to send
     * @param players The players who will receive the message
     */
    public static void sendMessage(String message, Player... players) {
        FancyMessage fancy = new FancyMessage(ChatColor.WHITE.toString());
        String[] split = message.split(" ");

        ChatColor current = ChatColor.WHITE;

        for (String str : split) {
            str = ChatColor.getLastColors(color(str)); // Color the word
            current = ChatColor.getByChar(str.isEmpty() ||str.equals("") ? current.getChar() : str.charAt(1)); // Change the last color

            if (str.equalsIgnoreCase("[item]")) {
                ChatColor color = ChatColor.getByChar(messageFile.getString("itemColor.color"));
                ChatColor style = messageFile.getString("itemColor.style").equalsIgnoreCase("none") ? null
                        : ChatColor.getByChar(messageFile.getString("itemColor.style"));
                // TODO: Get the item shit
            } else {
                fancy.then(str).color(current);
            }
        }

        // Send the message to the players
        for (Player player : players) {
            fancy.send(player);
        }
    }

    /**
     * Returns a string with it's colors formatted
     *
     * @param string The string to format
     *
     * @return The formatted string
     */
    public static String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /* Gets a string from the messages file */
    public static String getConfigMessage(String path) {
        return color(messageFile.getString(path));
    }

    /* Gets an items name */
    private static String getItemName(ItemStack item) {
        short durability = item.getType().getMaxDurability() > 0 ? 0 : item.getDurability();
        String search = item.getType().toString() + "." + durability;
        String ret = itemsFile.getString(search);

        return ret == null ? "null" : ret;
    }

    /* Replaces a String with Auction information */
    public static String replace(Auction auction, String message) {
        String ret = message;
        if (auction != null) {
            ret = ret.replaceAll("%t", auction.getTime())
                    .replaceAll("%b", NumberFormat.getInstance().format(auction.getTopBid()))
                    .replaceAll("%p", auction.getOwnerName())
                    .replaceAll("%a", Integer.toString(auction.getNumItems()))
                    .replaceAll("%A", NumberFormat.getInstance().format(auction.getAutoWin()));
            if (auction.hasBids()) {
                ret = ret.replaceAll("%T", Double.toString(auction.getCurrentTax()));
                        // .replaceAll("%w", UUIDtoName(auction.getWinning())); TODO: setWinningName
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', ret);
    }

    /**
     * Sends the auction menu to a sender
     * 
     * @param sender The sender to send the menu too
     */
    public static void sendMenu(CommandSender sender) {
        for (String message : messageFile.getStringList("auction-menu")) {
            sender.sendMessage(color(message));
        }
    }
}

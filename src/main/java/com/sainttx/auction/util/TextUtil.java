package com.sainttx.auction.util;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import mkremins.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class TextUtil {

    /*
     * The file containing all the configurable messages in the plugin
     */
    private static YamlConfiguration messageFile, messageFileDefault;

    /*
     * The file containing item names
     */
    private static YamlConfiguration itemsFile;

    /**
     * Loads the messages file and names file from a plugin
     */
    public static void load(JavaPlugin plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        File namesFile = new File(plugin.getDataFolder(), "items.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", true);
        }
        if (!namesFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        messageFileDefault = YamlConfiguration.loadConfiguration(plugin.getResource("messages.yml"));
        messageFile = YamlConfiguration.loadConfiguration(messagesFile);
        itemsFile = YamlConfiguration.loadConfiguration(namesFile);
    }

    /**
     * Saves the messages configuration to file
     */
    public static void save() {
        File messagesFile = new File(AuctionPlugin.getPlugin().getDataFolder(), "messages.yml");

        try {
            messageFile.save(messagesFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a fancy message ready to be sent
     *
     * @param auction the current auction
     * @param message the message to send
     * @return a message ready to be sent to a player
     */
    public static FancyMessage createMessage(Auction auction, String message) {
        FancyMessage fancy = new FancyMessage(ChatColor.WHITE.toString());

        if (!message.isEmpty()) {
            String[] split = message.split(" ");
            ChatColor current = ChatColor.WHITE;

            for (String str : split) {
                str = color(str); // Color the word
                String currentColor = ChatColor.getLastColors(str);
                current = ChatColor.getByChar(currentColor.isEmpty() ? current.getChar() : currentColor.charAt(1));

                if (str.toLowerCase().contains("%i")) {
                    ChatColor color = getConfigMessage("itemColor.color").isEmpty() ? ChatColor.WHITE : ChatColor.getByChar(getConfigMessage("itemColor.color"));
                    ChatColor style = getConfigMessage("itemColor.style").isEmpty() ? null : ChatColor.getByChar(getConfigMessage("itemColor.style"));

                    if (auction != null) {
                        fancy.then(getItemName(auction.getItem())).color(color != null ? current = color : current).itemTooltip(auction.getItem());
                        if (style != null && style.isFormat()) {
                            fancy.style(style);
                        }
                    }
                } else {
                    fancy.then(str);

                    if (current.isColor()) {
                        fancy.color(current);
                    } else {
                        fancy.style(current);
                    }
                }

                fancy.then(" "); // Add a space after every word
            }
        }

        return fancy;
    }

    /**
     * Returns a string with it's colors formatted
     *
     * @param string The string to format
     * @return The formatted string
     */
    public static String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    /**
     * Gets a string from the messages file
     *
     * @param path The path inside the message file
     */
    public static String getConfigMessage(String path) {
        if (messageFile.isString(path)) {
            return color(messageFile.getString(path));
        } else if (messageFileDefault.isString(path)) {
            Bukkit.getLogger().warning("Could not find auction message with path \"" + path + "\", using default instead");
            return color(messageFileDefault.getString(path));
        } else {
            Bukkit.getLogger().warning("Could not find auction message with path \"" + path + "\" anywhere, message ignored");
            return "";
        }
    }

    /*
     * Gets an items name
     */
    private static String getItemName(ItemStack item) {
        short durability = item.getType().getMaxDurability() > 0 ? 0 : item.getDurability();
        String search = item.getType().toString() + "." + durability;
        String ret = itemsFile.getString(search);

        return ret == null ? getMaterialName(item.getType()) : ret;
    }

    /*
     * Converts a material to a string (ie. ARMOR_STAND = Armor Stand)
     */
    private static String getMaterialName(Material material) {
        String[] split = material.toString().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();

        for (String str : split) {
            builder.append(str.substring(0, 1).toUpperCase() + str.substring(1) + " ");
        }

        return builder.toString().trim();
    }

    /*
     * Replaces a String with Auction information
     */
    public static String replace(Auction auction, String message) {
        String ret = message;
        if (auction != null) {
            int time = AuctionPlugin.getPlugin().getConfig().getInt("auctionSettings.antiSnipe.addSeconds", 5);
            ret = ret.replaceAll("%t", AuctionUtil.getFormattedTime(auction.getTimeLeft()))
                    .replaceAll("%b", NumberFormat.getInstance(Locale.ENGLISH).format(auction.getTopBid()))
                    .replaceAll("%p", auction.getOwnerName())
                    .replaceAll("%a", Integer.toString(auction.getItem().getAmount()))
                    .replaceAll("%A", NumberFormat.getInstance(Locale.ENGLISH).format(auction.getAutowin()))
                    .replaceAll("%B", Integer.toString((int) auction.getBidIncrement()))
                    .replaceAll("%s", Integer.toString(time));
            if (auction.getTopBidder() != null) {
                ret = ret.replaceAll("%T", Double.toString(auction.getTax()))
                        .replaceAll("%w", auction.getTopBidderName());
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

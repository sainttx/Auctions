package com.sainttx.auction.util;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.reward.ItemReward;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    /*
     * The file containing item names
     */
    private static YamlConfiguration itemsFile;

    /**
     * A pattern used to find chat colors in a string
     */
    public static final Pattern COLOR_FINDER_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "([a-f0-9klmnor])");

    /**
     * Loads the messages file and names file from a plugin
     */
    public static void load(JavaPlugin plugin) {
        File namesFile = new File(plugin.getDataFolder(), "items.yml");
        if (!namesFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        itemsFile = YamlConfiguration.loadConfiguration(namesFile);
    }


    /**
     * Creates a fancy message ready to be sent
     *
     * @param auction the current auction
     * @param message the message to send
     * @return a message ready to be sent to a player
     */
    public static FancyMessage createMessage(Auction auction, String message) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        FancyMessage fancy = new FancyMessage(ChatColor.WHITE.toString());

        if (!message.isEmpty()) {
            String[] split = message.split(" ");
            ChatColor current = ChatColor.WHITE;

            for (String str : split) {
                str = color(str); // Color the word
                String currentColor = ChatColor.getLastColors(str);
                current = ChatColor.getByChar(currentColor.isEmpty() ? current.getChar() : currentColor.charAt(1));

                if (str.contains("[item]") && auction != null) {
                    String rewardName = auction.getReward().getName();
                    String display = plugin.getMessage("messages.auctionFormattable.itemFormat");
                    display = ChatColor.translateAlternateColorCodes('&', display.replace("[itemName]", rewardName));

                    Set<ChatColor> colors = EnumSet.noneOf(ChatColor.class);
                    Matcher matcher = COLOR_FINDER_PATTERN.matcher(display);

                    while (matcher.find()) {
                        char cc = matcher.group(1).charAt(0);
                        colors.add(ChatColor.getByChar(cc));
                    }

                    fancy.then(ChatColor.stripColor(display));

                    if (auction.getReward() instanceof ItemReward) {
                        ItemReward item = (ItemReward) auction.getReward();
                        fancy.itemTooltip(item.getItem());
                    }

                    for (ChatColor color : colors) {
                        if (color.isColor()) {
                            fancy.color(color);
                        } else {
                            fancy.style(color);
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

    /*
     * Gets an items name
     */
    public static String getItemName(ItemStack item) {
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

    /**
     * Sends the auction menu to a sender
     *
     * @param sender The sender to send the menu too
     */
    public static void sendMenu(CommandSender sender) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();

        for (String message : plugin.getConfig().getStringList("messages.helpMenu")) {
            sender.sendMessage(color(message));
        }
    }
}

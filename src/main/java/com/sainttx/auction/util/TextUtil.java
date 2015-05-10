package com.sainttx.auction.util;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.reward.ItemReward;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    /**
     * A pattern used to find chat colors in a string
     */
    public static final Pattern COLOR_FINDER_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "([a-f0-9klmnor])");

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
                str = ChatColor.translateAlternateColorCodes('&', str); // Color the word
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
     * Sends the auction menu to a sender
     *
     * @param sender The sender to send the menu too
     */
    public static void sendMenu(CommandSender sender) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();

        for (String message : plugin.getConfig().getStringList("messages.helpMenu")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}

package com.sainttx.auction.structure.messages.handler;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.api.messages.MessageRecipientGroup;
import com.sainttx.auction.api.reward.ItemReward;
import com.sainttx.auction.util.TimeUtil;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A base message handler that handles message sending
 */
public class TextualMessageHandler implements MessageHandler {

    protected MessageFormatter formatter;
    protected Set<UUID> ignoring = new HashSet<UUID>();
    public static final Pattern COLOR_FINDER_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "([a-f0-9klmnor])");

    public TextualMessageHandler() {
        this.formatter = new MessageFormatterImpl();
    }

    @Override
    public void broadcast(String message, boolean force) {
        broadcast(null, message, force);
    }

    @Override
    public void broadcast(Auction auction, String message, boolean force) {
        message = formatter.format(message, auction);
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            FancyMessage fancy = createMessage(auction, msg);

            Collection<CommandSender> sentTo = new HashSet<CommandSender>();
            for (MessageRecipientGroup group : AuctionsAPI.getAuctionManager().getMessageGroups()) {
                for (CommandSender recipient : group.getRecipients()) {
                    if (sentTo.contains(recipient) || (recipient instanceof Player
                            && isIgnoring(((Player) recipient).getUniqueId()))) {
                        continue;
                    } else {
                        fancy.send(recipient);
                    }
                    sentTo.add(recipient);
                }
            }
        }
    }

    @Override
    public void sendMessage(String message, CommandSender recipient) {
        sendMessage(null, message, recipient);
    }

    @Override
    public void sendMessage(Auction auction, String message, CommandSender recipient) {
        message = formatter.format(message, auction);
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            FancyMessage fancy = createMessage(auction, msg);
            fancy.send(recipient);
        }
    }

    @Override
    public void sendAuctionInformation(CommandSender recipient, Auction auction) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        MessageHandler handler = AuctionsAPI.getMessageHandler();
        sendMessage(auction, plugin.getMessage("messages.auctionFormattable.info"), recipient);
        sendMessage(auction, plugin.getMessage("messages.auctionFormattable.increment"), recipient);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            int queuePosition = AuctionsAPI.getAuctionManager().getQueuePosition(player);
            if (queuePosition > 0) {
                String message = plugin.getMessage("messages.auctionFormattable.queuePosition")
                        .replace("[queuepos]", Integer.toString(queuePosition));
                handler.sendMessage(auction, message, player);
            }
        }
    }

    @Override
    public boolean isIgnoring(UUID playerId) {
        return ignoring.contains(playerId);
    }

    @Override
    public void addIgnoring(UUID playerId) {
        ignoring.add(playerId);
    }

    @Override
    public boolean removeIgnoring(UUID playerId) {
        return ignoring.remove(playerId);
    }

    /*
     * A helper method that creates a FancyMessage to send to players
     */
    private FancyMessage createMessage(Auction auction, String message) {
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
     * A message formatter that handles basic formatting
     */
    public static class MessageFormatterImpl implements MessageFormatter {

        @Override
        public String format(String message) {
            return format(message, null);
        }

        @Override
        public String format(String message, Auction auction) {
            if (message == null) {
                throw new IllegalArgumentException("message cannot be null");
            }
            if (auction != null) {
                message = message.replace("[itemamount]", Integer.toString(auction.getReward().getAmount()));
                message = message.replace("[time]", TimeUtil.getFormattedTime(auction.getTimeLeft()));
                message = message.replace("[autowin]", formatDouble(auction.getAutowin()));
                message = message.replace("[ownername]", auction.getOwnerName() == null ? "Console" : auction.getOwnerName());
                message = message.replace("[topbiddername]", auction.getTopBidderName() == null ? "Console" : auction.getTopBidderName());
                message = message.replace("[increment]", formatDouble(auction.getBidIncrement()));
                message = message.replace("[topbid]", formatDouble(auction.getTopBid()));
                message = message.replace("[taxpercent]", formatDouble(auction.getTax()));
                message = message.replace("[taxamount]", formatDouble(auction.getTaxAmount()));
                message = message.replace("[winnings]", formatDouble(auction.getTopBid() - auction.getTaxAmount()));
            }
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        /*
         * A helper method that formats numbers
         */
        private String formatDouble(double d) {
            NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(2);
            format.setMinimumFractionDigits(2);
            return format.format(d);
        }
    }
}

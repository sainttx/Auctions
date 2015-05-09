package com.sainttx.auction.api.messages;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.util.TextUtil;
import com.sainttx.auction.util.TimeUtil;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A base message handler that handles message sending
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    protected MessageFormatter formatter;
    protected static Set<UUID> ignoring = new HashSet<UUID>();

    public AbstractMessageHandler() {
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
            FancyMessage fancy = TextUtil.createMessage(auction, msg);

            for (CommandSender recipient : getRecipients()) {
                if (recipient instanceof Player && isIgnoring(((Player) recipient).getUniqueId())) {
                    continue;
                } else {
                    fancy.send(recipient);
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
            FancyMessage fancy = TextUtil.createMessage(auction, msg);
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

    @Override
    public MessageFormatter getFormatter() {
        return formatter;
    }

    @Override
    public abstract Iterable<? extends CommandSender> getRecipients();

    /**
     * A message formatter that handles basic formatting
     */
    public class MessageFormatterImpl implements MessageFormatter {

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

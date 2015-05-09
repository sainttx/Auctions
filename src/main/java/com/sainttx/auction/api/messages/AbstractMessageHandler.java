package com.sainttx.auction.api.messages;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.util.TextUtil;
import mkremins.fanciful.FancyMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
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
    public void sendMessage(String configurationPath, boolean force) {
        sendMessage(null, configurationPath, force);
    }

    @Override
    public void sendMessage(Auction auction, String configurationPath, boolean force) {
        String message = TextUtil.replace(auction, TextUtil.getConfigMessage(configurationPath));
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
    public void sendMessage(String configurationPath, CommandSender recipient) {
        sendMessage(null, configurationPath, recipient);
    }

    @Override
    public void sendMessage(Auction auction, String configurationPath, CommandSender recipient) {
        String message = TextUtil.replace(auction, TextUtil.getConfigMessage(configurationPath));
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            FancyMessage fancy = TextUtil.createMessage(auction, msg);
            fancy.send(recipient);
        }
    }

    @Override
    public void sendAuctionInformation(CommandSender recipient, Auction auction) {
        sendMessage(auction, "auction-info-message", recipient);
        sendMessage(auction, "auction-start-increment", recipient);

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            int queuePosition = AuctionsAPI.getAuctionManager().getQueuePosition(player);
            if (queuePosition > 0) {
                // TODO: Deprecated
                TextUtil.sendMessage(TextUtil.replace(auction, TextUtil.getConfigMessage("auction-queue-position")
                        .replaceAll("%q", Integer.toString(queuePosition))), true, player);
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
            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }
}

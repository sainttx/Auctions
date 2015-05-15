/*
 * Copyright (C) SainttX <http://sainttx.com>
 * Copyright (C) contributors
 *
 * This file is part of Auctions.
 *
 * Auctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Auctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Auctions.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sainttx.auctions.structure.messages.handler;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.messages.MessageHandlerAddon.SpammyMessagePreventer;
import com.sainttx.auctions.api.messages.MessageRecipientGroup;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.util.TimeUtil;
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
public class TextualMessageHandler implements MessageHandler, SpammyMessagePreventer {

    protected MessageFormatter formatter;
    protected Set<UUID> ignoring = new HashSet<UUID>();
    private Set<UUID> ignoringBids = new HashSet<UUID>();
    public static final Pattern COLOR_FINDER_PATTERN = Pattern.compile(ChatColor.COLOR_CHAR + "([a-f0-9klmnor])");

    public TextualMessageHandler() {
        this.formatter = new MessageFormatterImpl();
    }

    @Override
    public void broadcast(String message, boolean spammy) {
        broadcast(message, null, spammy);
    }

    @Override
    public void broadcast(String message, Auction auction, boolean spammy) {
        message = formatter.format(message, auction);
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            if (!msg.isEmpty()) {
                FancyMessage fancy = createMessage(auction, msg);

                for (CommandSender recipient : getAllRecipients()) {
                    if (recipient == null || (recipient instanceof Player
                            && isIgnoring((recipient)))) {
                        continue;
                    } else if (spammy && recipient instanceof Player
                            && isIgnoringSpammy(((Player) recipient).getUniqueId())) {
                        continue;
                    } else {
                        fancy.send(recipient);
                    }
                }
            }
        }
    }

    /**
     * Returns all registered recipients
     *
     * @return all recipients from the groups registered in {@link AuctionManager#getMessageGroups()}
     */
    protected Collection<CommandSender> getAllRecipients() {
        Collection<CommandSender> recipients = new HashSet<CommandSender>();
        for (MessageRecipientGroup group : AuctionsAPI.getAuctionManager().getMessageGroups()) {
            for (CommandSender recipient : group.getRecipients()) {
                recipients.add(recipient);
            }
        }

        return recipients;
    }

    @Override
    public void sendMessage(CommandSender recipient, String message) {
        sendMessage(recipient, message, null);
    }

    @Override
    public void sendMessage(CommandSender recipient, String message, Auction auction) {
        message = formatter.format(message, auction);
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            if (!msg.isEmpty() && recipient != null) {
                FancyMessage fancy = createMessage(auction, msg);
                fancy.send(recipient);
            }
        }
    }

    @Override
    public void sendAuctionInformation(CommandSender recipient, Auction auction) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        sendMessage(recipient, plugin.getMessage("messages.auctionFormattable.info"), auction);
        sendMessage(recipient, plugin.getMessage("messages.auctionFormattable.increment"), auction);
        if (auction.getTopBidder() != null) {
            sendMessage(recipient, plugin.getMessage("messages.auctionFormattable.infoTopBidder"), auction);
        }

        if (recipient instanceof Player) {
            Player player = (Player) recipient;
            int queuePosition = AuctionsAPI.getAuctionManager().getQueuePosition(player);
            if (queuePosition > 0) {
                String message = plugin.getMessage("messages.auctionFormattable.queuePosition")
                        .replace("[queuepos]", Integer.toString(queuePosition));
                sendMessage(player, message, auction);
            }
        }
    }

    @Override
    public boolean isIgnoring(CommandSender sender) {
        if (sender instanceof Player) {
            if (ignoring.contains(((Player) sender).getUniqueId())) {
                return true;
            }
        }
        return !getAllRecipients().contains(sender);
    }

    @Override
    public void addIgnoring(CommandSender sender) {
        if (sender instanceof Player) {
            ignoring.add(((Player) sender).getUniqueId());
        }
    }

    @Override
    public boolean removeIgnoring(CommandSender sender) {
        if (sender instanceof Player) {
            return ignoring.remove(((Player) sender).getUniqueId());
        } else {
            return false;
        }
    }

    @Override
    public void addIgnoringSpammy(UUID uuid) {
        ignoringBids.add(uuid);
    }

    @Override
    public void removeIgnoringSpammy(UUID uuid) {
        ignoringBids.remove(uuid);
    }

    @Override
    public boolean isIgnoringSpammy(UUID uuid) {
        return ignoringBids.contains(uuid);
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

        static final long THOUSAND = 1000L;
        static final long MILLION = 1000000L;
        static final long BILLION = 1000000000L;
        static final long TRILLION = 1000000000000L;

        @Override
        public String format(String message) {
            return format(message, null);
        }

        @Override
        public String format(String message, Auction auction) {
            if (message == null) {
                throw new IllegalArgumentException("message cannot be null");
            }
            AuctionPlugin plugin = AuctionPlugin.getPlugin();
            boolean truncate = plugin.getConfig().getBoolean("general.truncatedNumberFormat", false);

            if (auction != null) {
                if (auction.getType() == AuctionType.SEALED
                        && !auction.hasEnded()
                        && plugin.getConfig().getBoolean("auctionSettings.sealedAuctions.concealTopBidder", true)) {
                    message = message.replace("[topbiddername]", "Hidden")
                            .replace("[topbid]", "hidden");
                }
                message = message.replace("[itemName]", auction.getReward().getName());
                message = message.replace("[itemamount]", Integer.toString(auction.getReward().getAmount()));
                message = message.replace("[time]", TimeUtil.getFormattedTime(auction.getTimeLeft()));
                message = message.replace("[autowin]", truncate ? truncateNumber(auction.getAutowin()) : formatDouble(auction.getAutowin()));
                message = message.replace("[ownername]", auction.getOwnerName() == null ? "Console" : auction.getOwnerName());
                message = message.replace("[topbiddername]", auction.hasBids()
                        ? (auction.getTopBidderName() == null ? "Console" : auction.getTopBidderName())
                        : "Nobody");
                message = message.replace("[increment]", truncate ? truncateNumber(auction.getBidIncrement()) : formatDouble(auction.getBidIncrement()));
                message = message.replace("[topbid]", truncate ? truncateNumber(auction.getTopBid()) : formatDouble(auction.getTopBid()));
                message = message.replace("[taxpercent]", formatDouble(auction.getTax()));
                message = message.replace("[taxamount]", formatDouble(auction.getTaxAmount()));
                message = message.replace("[startprice]", truncate ? truncateNumber(auction.getStartPrice()) : formatDouble(auction.getStartPrice()));
                double winnings = auction.getTopBid() - auction.getTaxAmount();
                message = message.replace("[winnings]", truncate ? truncateNumber(winnings) : formatDouble(winnings));
            }
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        /*
         * A helper method that formats numbers
         */
        private String formatDouble(double d) {
            NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
            format.setMaximumFractionDigits(2);
            format.setMinimumFractionDigits(0);
            return format.format(d);
        }

        /*
         * A helper method to truncate numbers to the nearest amount
         */
        private String truncateNumber(double x) {
            return x < THOUSAND ? formatDouble(x) : x < MILLION ? formatDouble(x / THOUSAND) + "K" :
                    x < BILLION ? formatDouble(x / MILLION) + "M" : x < TRILLION ? formatDouble(x / BILLION) + "B" :
                            formatDouble(x / TRILLION) + "T";
        }
    }
}

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
import com.sainttx.auctions.api.messages.MessageRecipientGroup;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.util.ReflectionUtil;
import com.sainttx.auctions.util.TimeUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * A base message handler that handles message sending
 */
public class TextualMessageHandler implements MessageHandler {

    protected MessageFormatter formatter;
    protected Set<UUID> ignoring = new HashSet<UUID>();

    public TextualMessageHandler() {
        this.formatter = new MessageFormatterImpl();
    }

    @Override
    public void broadcast(String message, boolean force) {
        broadcast(message, null, force);
    }

    @Override
    public void broadcast(String message, Auction auction, boolean force) {
        message = formatter.format(message, auction);
        String[] messages = message.split("\n+");

        for (String msg : messages) {
            if (!msg.isEmpty()) {
                //FancyMessage fancy = createMessage(auction, msg);
                BaseComponent[] fancy = createMessage(msg, auction);

                for (CommandSender recipient : getAllRecipients()) {
                    if (recipient == null || (recipient instanceof Player
                            && isIgnoring((recipient)))) {
                        continue;
                    } else {
                        Player player = (Player) recipient;
                        player.spigot().sendMessage(fancy);
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
                BaseComponent[] baseComponents = createMessage(msg, auction);
                if (!(recipient instanceof Player)) {
                    recipient.sendMessage(TextComponent.toLegacyText(baseComponents));
                } else {
                    Player player = (Player) recipient;
                    player.spigot().sendMessage(baseComponents);
                }
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

    /**
     * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string
     * for sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
     *
     * @param itemStack the item to convert
     * @return the Json string representation of the item
     */
    private String convertItemStackToJson(ItemStack itemStack) {
        // ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
        Class<?> craftItemStackClazz = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
        Method asNMSCopyMethod = ReflectionUtil.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

        // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
        Class<?> nmsItemStackClazz = ReflectionUtil.getNMSClass("ItemStack");
        Class<?> nbtTagCompoundClazz = ReflectionUtil.getNMSClass("NBTTagCompound");
        Method saveNmsItemStackMethod = ReflectionUtil.getMethod(nmsItemStackClazz, "save", nbtTagCompoundClazz);

        Object nmsNbtTagCompoundObj; // This will just be an empty NBTTagCompound instance to invoke the saveNms method
        Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack object received from the asNMSCopy method
        Object itemAsJsonObject; // This is the net.minecraft.server.ItemStack after being put through saveNmsItem method

        try {
            nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
            nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
            itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.SEVERE, "failed to serialize itemstack to nms item", t);
            return null;
        }

        // Return a string representation of the serialized object
        return itemAsJsonObject.toString();
    }

    /*
     * Turns the message into BaseComponents using the ChatComponent API
     */
    private BaseComponent[] createMessage(String message, Auction auction) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        BaseComponent[] components = TextComponent.fromLegacyText(message);

        for (BaseComponent component : components) {
            if (component instanceof TextComponent) {
                TextComponent textComponent = (TextComponent) component;
                if (textComponent.getText().contains("[item]") && auction != null) {
                    String rewardName = auction.getReward().getName();
                    String display = plugin.getMessage("messages.auctionFormattable.itemFormat");
                    display = ChatColor.translateAlternateColorCodes('&', display.replace("[itemName]", rewardName));

                    textComponent.setText(textComponent.getText().replace("[item]", display));

                    if (auction.getReward() instanceof ItemReward) {
                        ItemReward reward = (ItemReward) auction.getReward();
                        ItemStack itemStack = reward.getItem();
                        String itemJson = convertItemStackToJson(itemStack);

                        // Prepare a BaseComponent array with the itemJson as a text component
                        BaseComponent[] hoverEventComponents = new BaseComponent[]{
                                new TextComponent(itemJson) // The only element of the hover events basecomponents is the item json
                        };

                        // Create the hover event
                        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverEventComponents);
                        textComponent.setHoverEvent(event);
                    }
                }
            }
        }
        return components;
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
                if (auction.getType() == AuctionType.SEALED && !auction.hasEnded()
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

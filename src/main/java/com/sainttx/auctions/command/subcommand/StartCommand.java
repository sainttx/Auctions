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

package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.command.AuctionSubCommand;
import com.sainttx.auctions.inventory.AuctionInventory;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction start command for the auction plugin
 */
public class StartCommand extends AuctionSubCommand {

    public StartCommand() {
        super("auctions.command.start", "start", "s", "star");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can start auctions");
        } else if (args.length < 2) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.startSyntax"));
        } else if (manager.isAuctioningDisabled() && !sender.hasPermission("auctions.bypass.general.disabled")) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.auctionsDisabled"));
        } else if (!plugin.getConfig().getBoolean("auctionSettings.sealedAuctions.enabled", false)
                && cmd.getName().equalsIgnoreCase("sealedauction")) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.sealedAuctionsDisabled"));
        } else {
            Player player = (Player) sender;

            if (handler.isIgnoring(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.currentlyIgnoring")); // player is ignoring
            } else if (player.getGameMode() == GameMode.CREATIVE
                    && !plugin.getConfig().getBoolean("auctionSettings.canAuctionInCreative", false)
                    && !player.hasPermission("auctions.bypass.general.creative")) {
                handler.sendMessage(player, plugin.getMessage("messages.error.creativeNotAllowed"));
            } else {
                double price; // the starting cost

                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException ex) {
                    handler.sendMessage(sender, plugin.getMessage("messages.error.invalidNumberEntered"));
                    return true;
                }

                if (Double.isInfinite(price) || Double.isNaN(price)) {
                    handler.sendMessage(sender, plugin.getMessage("messages.error.invalidNumberEntered")); // invalid number
                } else if (price < plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.startPriceTooLow")); // starting price too low
                } else if (!player.hasPermission("auctions.bypass.start.maxprice")
                        && price > plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.startPriceTooHigh")); // starting price too high
                } else if (manager.getQueue().size() >= plugin.getConfig().getInt("auctionSettings.auctionQueueLimit", 3)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.auctionQueueFull")); // queue full
                } else if (manager.hasActiveAuction(player)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.alreadyHaveAuction"));
                } else if (manager.hasAuctionInQueue(player)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.alreadyInAuctionQueue"));
                } else {
                    AuctionInventory inventory = new AuctionInventory("Auction Inv", player.getUniqueId(), price);
                    inventory.open(player);
                }
            }
        }

        return false;
    }
}

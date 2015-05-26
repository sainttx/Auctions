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

package com.sainttx.auctions.listener;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.inventory.AuctionInventory;
import com.sainttx.auctions.structure.module.AntiSnipeModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Created by Matthew on 25/05/2015.
 */
public class InventoryListener implements Listener {

    private AuctionPlugin plugin;

    public InventoryListener(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        MessageHandler handler = AuctionsAPI.getMessageHandler();

        if (inventory.getHolder() instanceof AuctionInventory) {
            AuctionInventory inv = (AuctionInventory) inventory.getHolder();
            Player player = Bukkit.getPlayer(inv.getPlayerId());
            ItemStack compare = null;
            boolean unsimilarItem = false; // TODO: Remove?

            for (ItemStack i : inventory.getContents()) {
                if (i != null && i.getType() != Material.AIR) {
                    if (compare == null) {
                        compare = i.clone();
                    } else if (!compare.isSimilar(i)) {
                        // Can't have different items
                        unsimilarItem = true;
                        break;
                    } else {
                        compare.setAmount(compare.getAmount() + i.getAmount());
                    }
                }
            }

            if (compare != null) {
                ItemReward reward = new ItemReward(compare);

                if (unsimilarItem) {
                    handler.sendMessage(player, "messages.error.differentItem");

                    for (ItemStack itm : inventory.getContents()) {
                        if (itm != null) {
                            player.getWorld().dropItem(player.getLocation(), itm);
                        }
                    }
                } else {
                    AuctionManager manager = AuctionsAPI.getAuctionManager();
                    ItemStack item = compare;
                    double fee = plugin.getConfig().getDouble("auctionSettings.startFee", 0);

                    if (!player.hasPermission("auctions.bypass.general.bannedmaterial")
                            && manager.isBannedMaterial(item.getType())) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.invalidItemType")); // item type not allowed
                    } else if (!player.hasPermission("auctions.bypass.general.damageditems")
                            && item.getType().getMaxDurability() > 0 && item.getDurability() > 0
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionDamagedItems", true)) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionDamagedItems")); // can't auction damaged
                    } else if (!player.hasPermission("auctions.bypass.general.nameditems")
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionNamedItems", true)
                            && item.getItemMeta().hasDisplayName()) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionNamedItems")); // cant auction named
                    } else if (!player.hasPermission("auctions.bypass.general.bannedlore") && hasBannedLore(item)) {
                        // The players item contains a piece of denied lore
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionBannedLore"));
                    } else if (fee > plugin.getEconomy().getBalance(player)) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.insufficientBalance")); // not enough funds
                    } else {
                        Auction.Builder builder = AuctionsAPI.getAuctionBuilder(AuctionType.STANDARD);
                        builder.reward(reward);
                        builder.owner(player);
                        builder.topBid(inv.getStartPrice());
                        Auction created = builder.build();

                        // check if we can add an anti snipe module
                        if (plugin.getConfig().getBoolean("auctionSettings.antiSnipe.enabled", true)) {
                            created.addModule(new AntiSnipeModule(created));
                        }

                        plugin.getEconomy().withdrawPlayer(player, fee); // withdraw the start fee

                        if (manager.canStartNewAuction()) {
                            manager.setCurrentAuction(created);
                            created.start();
                            manager.setCanStartNewAuction(false);
                        } else {
                            manager.addAuctionToQueue(created);
                            handler.sendMessage(player, plugin.getMessage("messages.auctionPlacedInQueue"));
                        }
                        return;
                    }

                    // Return the item
                    reward.giveItem(player);
                }
            }
        }
    }

    /**
     * Checks if an item has a banned piece of lore
     *
     * @param item the item
     * @return true if the item has a banned piece of lore
     */
    public boolean hasBannedLore(ItemStack item) {
        List<String> bannedLore = plugin.getConfig().getStringList("general.blockedLore");

        if (bannedLore != null && !bannedLore.isEmpty()) {
            if (item.getItemMeta().hasLore()) {
                List<String> lore = item.getItemMeta().getLore();

                for (String loreItem : lore) {
                    for (String banned : bannedLore) {
                        if (loreItem.contains(banned)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlerPlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        handlerPlayerLeave(event.getPlayer());
    }

    /*
     * A helper method that takes care of players leaving
     */
    private void handlerPlayerLeave(Player player) {
        InventoryView inv = player.getOpenInventory();

        if (inv != null && inv.getTopInventory().getHolder() instanceof AuctionInventory) {
            Inventory inventory = inv.getTopInventory();

            for (ItemStack itm : inventory.getContents()) {
                if (itm != null) {
                    player.getInventory().addItem(itm); // TODO: Full inventory
                }
            }
            player.closeInventory();
        }
    }
}

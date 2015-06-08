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
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.reward.Reward;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Monitors specific events for the auction plugin
 */
public class PlayerListener implements Listener {

    private AuctionPlugin plugin;

    public PlayerListener(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    /**
     * Responsible for giving the players back items that were unable to be
     * returned at a previous time
     */
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Reward reward = plugin.getOfflineReward(player.getUniqueId());

        if (reward != null) {
            plugin.getLogger().info("Giving back saved items of offline player "
                    + player.getName() + " (uuid: " + player.getUniqueId() + ")");
            AuctionsAPI.getMessageHandler().sendMessage(player, plugin.getMessage("messages.savedItemReturn"));
            reward.giveItem(player);
            plugin.removeOfflineReward(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    /**
     * Cancels a players command if they're auctioning
     */
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0];
        if (!player.hasPermission("auctions.bypass.general.blockedcommands")
                && plugin.getConfig().isList("general.blockedCommands")
                && plugin.getConfig().getStringList("general.blockedCommands").contains(command.toLowerCase())) {
            Auction auction = AuctionsAPI.getAuctionManager().getCurrentAuction();

            if (plugin.getConfig().getBoolean("general.blockCommands.ifAuctioning", false)
                    && AuctionsAPI.getAuctionManager().hasActiveAuction(player)) {
                event.setCancelled(true);
                AuctionsAPI.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantUseCommandWhileAuctioning"));
            } else if (plugin.getConfig().getBoolean("general.blockCommands.ifQueued", false)
                    && AuctionsAPI.getAuctionManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                AuctionsAPI.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantUseCommandWhileQueued"));
            } else if (plugin.getConfig().getBoolean("general.blockCommands.ifTopBidder", false)
                    && auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                event.setCancelled(true);
                AuctionsAPI.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantUseCommandWhileTopBidder"));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World target = event.getTo().getWorld();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && !player.hasPermission("auctions.bypass.general.disabledworld")
                && plugin.isWorldDisabled(target)) {
            if (AuctionsAPI.getAuctionManager().hasActiveAuction(player)
                    || AuctionsAPI.getAuctionManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                AuctionsAPI.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantTeleportToDisabledWorld"));
            } else {
                Auction auction = AuctionsAPI.getAuctionManager().getCurrentAuction();

                if (auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                    event.setCancelled(true);
                    AuctionsAPI.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantTeleportToDisabledWorld"));
                }
            }
        }
    }
}

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

import com.sainttx.auctions.AuctionPluginImpl;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/**
 * Monitors specific events for the auction plugin
 */
public class PlayerListener implements Listener {

    private AuctionPluginImpl plugin;

    public PlayerListener(AuctionPluginImpl plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    /**
     * Responsible for giving the players back items that were unable to be
     * returned at a previous time
     */
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Reward reward = plugin.getOfflineReward(player.getUniqueId());

        if (reward != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Player player1 = Bukkit.getPlayer(uuid);

                if (player1 != null) {
                    plugin.getLogger().info("Giving back saved items of offline player "
                            + player1.getName() + " (uuid: " + player1.getUniqueId() + ")");
                    plugin.getMessageHandler().sendMessage(player1, plugin.getMessage("messages.savedItemReturn"));
                    reward.giveItem(player1);
                    plugin.removeOfflineReward(player1.getUniqueId());
                }
            }, plugin.getConfig().getLong("general.offlineRewardTickDelay"));
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
            if (plugin.getConfig().getBoolean("general.blockCommands.ifAuctioning", false)
                    && plugin.getManager().hasActiveAuction(player)) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantUseCommandWhileAuctioning"));
            } else if (plugin.getConfig().getBoolean("general.blockCommands.ifQueued", false)
                    && plugin.getManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantUseCommandWhileQueued"));
            } else if (plugin.getConfig().getBoolean("general.blockCommands.ifTopBidder", false)
                    && plugin.getManager().getCurrentAuction() != null && player.getUniqueId().equals(plugin.getManager().getCurrentAuction().getTopBidder())) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantUseCommandWhileTopBidder"));
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
            if (plugin.getManager().hasActiveAuction(player)
                    || plugin.getManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantTeleportToDisabledWorld"));
            } else {
                Auction auction = plugin.getManager().getCurrentAuction();

                if (auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                    event.setCancelled(true);
                    plugin.getMessageHandler().sendMessage(player, plugin.getMessage("messages.error.cantTeleportToDisabledWorld"));
                }
            }
        }
    }
}

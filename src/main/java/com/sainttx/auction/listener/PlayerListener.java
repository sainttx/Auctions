package com.sainttx.auction.listener;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.reward.Reward;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Created by Matthew on 09/05/2015.
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
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Reward reward = plugin.getOfflineReward(player.getUniqueId());

        if (reward != null) {
            reward.giveItem(player);
            AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("saved-item-return", player);
            plugin.removeOfflineReward(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    /**
     * Cancels a players command if they're auctioning
     */
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split(" ")[0];
        if (plugin.getConfig().getBoolean("general.blockCommands.ifAuctioning", false)
                && plugin.getConfig().isList("general.blockedCommands")
                && plugin.getConfig().getStringList("general.blockedCommands").contains(command.toLowerCase())) {
            Player player = event.getPlayer();
            Auction auction = AuctionsAPI.getAuctionManager().getCurrentAuction();

            if (AuctionsAPI.getAuctionManager().hasActiveAuction(player)) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("command-blocked-auctioning", player);
            } else if (plugin.getConfig().getBoolean("general.blockedCommands.ifQueued", false)
                    && AuctionsAPI.getAuctionManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("command-blocked-auction-queued", player);
            } else if (plugin.getConfig().getBoolean("general.blockCommands.ifTopBidder", false)
                    && auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("command-blocked-top-bidder", player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World target = event.getTo().getWorld();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && plugin.getConfig().isList("general.disabledWorlds")
                && plugin.getConfig().getStringList("general.disabledWorlds").contains(target.getName())) {
            if (AuctionsAPI.getAuctionManager().hasActiveAuction(player)
                    || AuctionsAPI.getAuctionManager().hasAuctionInQueue(player)) {
                event.setCancelled(true);
                AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("fail-teleport-world-disabled", player);
            } else {
                Auction auction = AuctionsAPI.getAuctionManager().getCurrentAuction();

                if (auction != null && player.getUniqueId().equals(auction.getTopBidder())) {
                    event.setCancelled(true);
                    AuctionsAPI.getAuctionManager().getMessageHandler().sendMessage("fail-teleport-world-disabled", player);
                }
            }
        }
    }
}

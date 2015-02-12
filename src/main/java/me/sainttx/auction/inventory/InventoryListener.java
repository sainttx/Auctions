package me.sainttx.auction.inventory;

import me.sainttx.auction.Auction;
import me.sainttx.auction.AuctionManager;
import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.util.AuctionUtil;
import me.sainttx.auction.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Created by Matthew on 29/01/2015.
 */
public class InventoryListener implements Listener {

    /*
     * The Auction plugin instance
     */
    private AuctionPlugin plugin;

    /**
     * Creates the listener
     */
    public InventoryListener(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof AuctionInventory) {
            AuctionInventory inv = (AuctionInventory) inventory.getHolder();
            ItemStack[] contents = inventory.getContents();
            ItemStack compare = null;
            int totalAmount = 0;
            boolean diff = false;

            for (ItemStack i : contents) {
                if (i != null && i.getType() != Material.AIR) {
                    if (compare == null) {
                        compare = i.clone();
                        totalAmount += compare.getAmount();
                    } else if (!compare.isSimilar(i)) {
                        // Can't have different items
                        diff = true;
                        break;
                    } else {
                        totalAmount += i.getAmount();
                    }
                }
            }

            if (compare == null) {
                return;
            }

            Player player = Bukkit.getPlayer(inv.getPlayerId());
            if (player == null) {
                // It won't be
                return;
            }

            boolean dropped = false;
            if (diff || player.hasMetadata("leaving")) {
                TextUtil.sendMessage(!diff ? "" : TextUtil.getConfigMessage("different-item"), true, player);
                for (ItemStack itm : contents) {
                    if (itm != null) {
                        if (AuctionUtil.hasSpace(player.getInventory(), itm)) {
                            player.getInventory().addItem(itm);
                        } else {
                            player.getWorld().dropItem(player.getLocation(), itm);
                            dropped = true;
                        }
                    }
                }
                player.updateInventory();
                if (dropped) {
                    TextUtil.sendMessage(TextUtil.getConfigMessage("items-no-space"), true, player);
                }
            } else {
                AuctionManager manager = AuctionManager.getAuctionManager();
                Auction auction = manager.createAuction(player, compare, totalAmount, inv.getStartPrice(), plugin.getConfig().getInt("auction-bid-increment", 100));

                if (manager.getCurrentAuction() == null && manager.canAuction()) {
                    manager.startAuction(auction);
                } else {
                    manager.queueAuction(auction);
                    TextUtil.sendMessage(TextUtil.getConfigMessage("auction-queued"), true, player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getOpenInventory() != null
                && event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof AuctionInventory) {
            event.getPlayer().setMetadata("leaving", new FixedMetadataValue(plugin, true));
            event.getPlayer().closeInventory();
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.getPlayer().getOpenInventory() != null
                && event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof AuctionInventory) {
            event.getPlayer().setMetadata("leaving", new FixedMetadataValue(plugin, true));
            event.getPlayer().closeInventory();
        }
    }
}

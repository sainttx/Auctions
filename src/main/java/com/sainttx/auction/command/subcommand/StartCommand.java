package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionType;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.reward.ItemReward;
import com.sainttx.auction.api.reward.Reward;
import com.sainttx.auction.command.AuctionSubCommand;
import com.sainttx.auction.structure.module.AntiSnipeModule;
import com.sainttx.auction.structure.module.AutoWinModule;
import com.sainttx.auction.util.AuctionUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Created by Matthew on 09/05/2015.
 */
public class StartCommand extends AuctionSubCommand {

    public StartCommand() {
        super("auctions.start", "start", "s", "star");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can start auctions");
        } else if (args.length < 3) {
            manager.getMessageHandler().sendMessage("fail-start-syntax", sender);
        } else if (manager.isAuctioningDisabled() && !sender.hasPermission("auction.bypass.disable")) {
            manager.getMessageHandler().sendMessage("fail-start-auction-disabled", sender);
        } else {
            Player player = (Player) sender;
            double fee = plugin.getConfig().getDouble("auctionSettings.startFee", 0);

            if (fee > plugin.getEconomy().getBalance(player)) {
                manager.getMessageHandler().sendMessage("fail-start-no-funds", player); // not enough funds
            } else if (player.getGameMode() == GameMode.CREATIVE
                    && !plugin.getConfig().getBoolean("auctionSettings.canAuctionInCreative", false)
                    && !player.hasPermission("auction.creative")) {
                manager.getMessageHandler().sendMessage("fail-start-creative", player);
            } else {
                Auction.Builder builder;

                if (cmd.getName().equals("silentauction")) {
                    builder = AuctionsAPI.getAuctionBuilder(AuctionType.SILENT);
                } else {
                    builder = AuctionsAPI.getAuctionBuilder(AuctionType.STANDARD);
                }

                int amount; // the amount of items to auction
                double price; // the starting cost
                int increment = -1;
                double autowin = -1;

                try {
                    amount = Integer.parseInt(args[1]);
                    price = Double.parseDouble(args[2]);

                    if (args.length > 3) {
                        increment = Integer.parseInt(args[3]);
                    }
                    if (args.length > 4) {
                        autowin = Double.parseDouble(args[4]);
                    }
                } catch (NumberFormatException ex) {
                    manager.getMessageHandler().sendMessage("fail-number-format", sender);
                    return true;
                }

                if (amount < 0) {
                    manager.getMessageHandler().sendMessage("fail-start-negative-number", player); // negative amount
                } else if (amount > 2304) {
                    manager.getMessageHandler().sendMessage("fail-start-not-enough-items", player); // not enough
                } else if (Double.isInfinite(price) || Double.isNaN(price) || Double.isInfinite(autowin) || Double.isNaN(autowin)) {
                    manager.getMessageHandler().sendMessage("fail-number-format", sender); // invalid number
                } else if (price < plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0)) {
                    manager.getMessageHandler().sendMessage("fail-start-min", player); // starting price too low
                } else if (price > plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999)) {
                    manager.getMessageHandler().sendMessage("fail-start-max", player); // starting price too high
                } else if (manager.getQueue().size() >= plugin.getConfig().getInt("auctionSettings.auctionQueueLimit", 3)) {
                    manager.getMessageHandler().sendMessage("fail-start-queue-full", player); // queue full
                } else if (increment != -1 && (increment < plugin.getConfig().getInt("auctionSettings.minimumBidIncrement", 10)
                        || increment > plugin.getConfig().getInt("auctionSettings.maximumBidIncrement", 9999))) {
                    manager.getMessageHandler().sendMessage("fail-start-bid-increment", player);
                } else if (autowin != -1 && !plugin.getConfig().getBoolean("auctionSettings.canSpecifyAutowin", true)) {
                    manager.getMessageHandler().sendMessage("fail-start-no-autowin", player);
                } else if (manager.hasActiveAuction(player)) {
                    manager.getMessageHandler().sendMessage("fail-start-already-auctioning", player);
                } else if (manager.hasAuctionInQueue(player)) {
                    manager.getMessageHandler().sendMessage("fail-start-already-queued", player);
                } else {
                    ItemStack item = player.getItemInHand().clone();

                    if (item == null || item.getType() == Material.AIR) {
                        manager.getMessageHandler().sendMessage("fail-start-hand-empty", player); // auctioned nothing
                    } else if (manager.isBannedMaterial(item.getType())) {
                        manager.getMessageHandler().sendMessage("unsupported-item", player); // item type not allowed
                    } else if (item.getType().getMaxDurability() > 0 && item.getDurability() > 0
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionDamagedItems", true)) {
                        manager.getMessageHandler().sendMessage("fail-start-damaged-item", player); // can't auction damaged
                    } else if (!AuctionUtil.searchInventory(player.getInventory(), item, amount)) {
                        manager.getMessageHandler().sendMessage("fail-start-not-enough-items", player);
                    } else if (!plugin.getConfig().getBoolean("auctionSettings.canAuctionNamedItems", true)
                            && item.getItemMeta().hasDisplayName()) {
                        manager.getMessageHandler().sendMessage("fail-start-named-item", player); // cant auction named
                    }  else if (hasBannedLore(item)) {
                        // The players item contains a piece of denied lore
                        manager.getMessageHandler().sendMessage("fail-start-banned-lore", player);
                    } else {
                        Reward reward = new ItemReward(item);
                        builder.bidIncrement(increment).reward(reward).owner(player).topBid(price).autowin(autowin);
                        Auction created = builder.build();

                        // check if we can add an autowin module
                        if (autowin != -1) {
                            created.addModule(new AutoWinModule(created, autowin));
                            manager.getMessageHandler().broadcast(created, "auction-start-autowin", false);
                        }
                        // check if we can add an anti snipe module
                        if (plugin.getConfig().getBoolean("auctionSettings.antiSnipe.enabled", true)) {
                            created.addModule(new AntiSnipeModule(created));
                        }

                        player.getInventory().removeItem(item); // take the item from the player
                        plugin.getEconomy().withdrawPlayer(player, fee); // withdraw the start fee

                        if (manager.canStartNewAuction()) {
                            manager.setCurrentAuction(created);
                            created.start();
                            manager.setCanStartNewAuction(false);
                        } else {
                            manager.addAuctionToQueue(created);
                            manager.getMessageHandler().sendMessage("auction-queued", player);
                        }
                    }
                }
            }
        }

        return false;
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
}

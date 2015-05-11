package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.reward.ItemReward;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.command.AuctionSubCommand;
import com.sainttx.auctions.structure.module.AntiSnipeModule;
import com.sainttx.auctions.structure.module.AutoWinModule;
import com.sainttx.auctions.util.AuctionUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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
        } else if (args.length < 3) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.startSyntax"));
        } else if (manager.isAuctioningDisabled() && !sender.hasPermission("auctions.bypass.general.disabled")) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.auctionsDisabled"));
        } else if (!plugin.getConfig().getBoolean("auctionSettings.sealedAuctions.enabled", false)
                && cmd.getName().equalsIgnoreCase("sealedauction")) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.sealedAuctionsDisabled"));
        } else {
            Player player = (Player) sender;
            double fee = plugin.getConfig().getDouble("auctionSettings.startFee", 0);

            if (handler.isIgnoring(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.currentlyIgnoring")); // player is ignoring
            } else if (fee > plugin.getEconomy().getBalance(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.insufficientBalance")); // not enough funds
            } else if (player.getGameMode() == GameMode.CREATIVE
                    && !plugin.getConfig().getBoolean("auctionSettings.canAuctionInCreative", false)
                    && !player.hasPermission("auctions.bypass.general.creative")) {
                handler.sendMessage(player, plugin.getMessage("messages.error.creativeNotAllowed"));
            } else {
                Auction.Builder builder;

                if (cmd.getName().equals("sealedauction")) {
                    builder = AuctionsAPI.getAuctionBuilder(AuctionType.SEALED);
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
                    handler.sendMessage(sender, plugin.getMessage("messages.error.invalidNumberEntered"));
                    return true;
                }

                if (amount < 0) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.invalidNumberEntered")); // negative amount
                } else if (amount > 2304) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.notEnoughOfItem")); // not enough
                } else if (Double.isInfinite(price) || Double.isNaN(price) || Double.isInfinite(autowin) || Double.isNaN(autowin)) {
                    handler.sendMessage(sender, plugin.getMessage("messages.error.invalidNumberEntered")); // invalid number
                } else if (price < plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.startPriceTooLow")); // starting price too low
                } else if (!player.hasPermission("auctions.bypass.start.maxprice")
                        && price > plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.startPriceTooHigh")); // starting price too high
                } else if (manager.getQueue().size() >= plugin.getConfig().getInt("auctionSettings.auctionQueueLimit", 3)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.auctionQueueFull")); // queue full
                } else if (increment != -1 && (increment < plugin.getConfig().getInt("auctionSettings.minimumBidIncrement", 10)
                        || increment > plugin.getConfig().getInt("auctionSettings.maximumBidIncrement", 9999))) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.invalidBidIncrement"));
                } else if (autowin != -1 && !plugin.getConfig().getBoolean("auctionSettings.canSpecifyAutowin", true)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.autowinDisabled"));
                } else if (manager.hasActiveAuction(player)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.alreadyHaveAuction"));
                } else if (manager.hasAuctionInQueue(player)) {
                    handler.sendMessage(player, plugin.getMessage("messages.error.alreadyInAuctionQueue"));
                } else {
                    ItemStack item = player.getItemInHand().clone();

                    if (item == null || item.getType() == Material.AIR) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.invalidItemType")); // auctioned nothing
                    } else if (!player.hasPermission("auctions.bypass.general.bannedmaterial")
                            && manager.isBannedMaterial(item.getType())) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.invalidItemType")); // item type not allowed
                    } else if (!player.hasPermission("auctions.bypass.general.damageditems")
                            && item.getType().getMaxDurability() > 0 && item.getDurability() > 0
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionDamagedItems", true)) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionDamagedItems")); // can't auction damaged
                    } else if (AuctionUtil.getAmountItems(player.getInventory(), item) < amount) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.notEnoughOfItem"));
                    } else if (!player.hasPermission("auctions.bypass.general.nameditems")
                            && !plugin.getConfig().getBoolean("auctionSettings.canAuctionNamedItems", true)
                            && item.getItemMeta().hasDisplayName()) {
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionNamedItems")); // cant auction named
                    } else if (!player.hasPermission("auctions.bypass.general.bannedlore") && hasBannedLore(item)) {
                        // The players item contains a piece of denied lore
                        handler.sendMessage(player, plugin.getMessage("messages.error.cantAuctionBannedLore"));
                    } else {
                        item.setAmount(amount);
                        Reward reward = new ItemReward(item);
                        builder.bidIncrement(increment).reward(reward).owner(player).topBid(price).autowin(autowin);
                        Auction created = builder.build();

                        // check if we can add an autowin module
                        if (created.getAutowin() != -1) {
                            created.addModule(new AutoWinModule(created, autowin));
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
                            handler.sendMessage(player, plugin.getMessage("messages.auctionPlacedInQueue"));
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

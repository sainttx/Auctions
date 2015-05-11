package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionType;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction bid command for the auction plugin
 */
public class BidCommand extends AuctionSubCommand {

    public BidCommand() {
        super("auctions.command.bid", "bid", "b");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();
        Auction auction = manager.getCurrentAuction();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can place bids on auctions");
        } else if (args.length < 2 && !plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true)) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.bidSyntax"));
        } else if (auction == null) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (auction.getType() == AuctionType.SEALED && args.length < 2) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.bidSyntax"));
        } else {
            Player player = (Player) sender;

            double bid;

            try {
                bid = args.length < 2
                        ? auction.getTopBid() + auction.getBidIncrement()
                        : Double.parseDouble(args[1]);
            } catch (NumberFormatException ex) {
                handler.sendMessage(sender, plugin.getMessage("messages.error.invalidNumberEntered"));
                return true;
            }

            if (!player.hasPermission("auctions.bypass.general.disabledworld")
                    && plugin.getConfig().isList("general.disabledWorlds")
                    && plugin.getConfig().getStringList("general.disabledWorlds").contains(player.getWorld().getName())) {
                handler.sendMessage(player, plugin.getMessage("messages.error.cantUsePluginInWorld"));
            } else if (handler.isIgnoring(player)) {
                handler.sendMessage(player, plugin.getMessage("messages.error.currentlyIgnoring")); // player is ignoring
            } else if (auction.getOwner().equals(player.getUniqueId())) {
                handler.sendMessage(player, plugin.getMessage("messages.error.bidOnOwnAuction")); // cant bid on own auction
            } else {
                auction.placeBid(player, bid);
            }
        }

        return true;
    }
}

package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction bid command for the auction plugin
 */
public class BidCommand extends AuctionSubCommand {

    public BidCommand() {
        super("auctions.bid", "bid", "b");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();
        Auction auction = manager.getCurrentAuction();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can place bids on auctions");
        } else if (args.length < 2 && !plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true)) {
            handler.sendMessage(plugin.getMessage("messages.error.bidSyntax"), sender);
        } else if (auction == null) {
            handler.sendMessage(plugin.getMessage("messages.error.noCurrentAuction"), sender);
        } else {
            Player player = (Player) sender;

            double bid;

            try {
                bid = args.length < 2
                        ? auction.getTopBid() + auction.getBidIncrement()
                        : Double.parseDouble(args[1]);
            } catch (NumberFormatException ex) {
                handler.sendMessage(plugin.getMessage("messages.error.invalidNumberEntered"), sender);
                return true;
            }

            if (plugin.getConfig().isList("general.disabledWorlds")
                    && plugin.getConfig().getStringList("general.disabledWorlds").contains(player.getWorld().getName())) {
                handler.sendMessage(plugin.getMessage("messages.error.cantUsePluginInWorld"), player);
            } else if (handler.isIgnoring(player.getUniqueId())) {
                handler.sendMessage(plugin.getMessage("messages.error.currentlyIgnoring"), player); // player is ignoring
            } else if (auction.getOwner().equals(player.getUniqueId())) {
                handler.sendMessage(plugin.getMessage("messages.error.bidOnOwnAuction"), player); // cant bid on own auction
            } else if (bid < auction.getTopBid() + auction.getBidIncrement()) {
                handler.sendMessage(plugin.getMessage("messages.error.bidTooLow"), player); // the bid wasnt enough
            } else if (plugin.getEconomy().getBalance(player) < bid) {
                handler.sendMessage(plugin.getMessage("messages.error.insufficientBalance"), player); // insufficient funds
            } else if (player.getUniqueId().equals(auction.getTopBidder())) {
                handler.sendMessage(plugin.getMessage("messages.error.alreadyTopBidder"), player); // already top bidder
            } else {
                if (auction.getTopBidder() != null) { // give the old winner their money back
                    OfflinePlayer oldPlayer = Bukkit.getOfflinePlayer(auction.getTopBidder());
                    plugin.getEconomy().depositPlayer(oldPlayer, auction.getTopBid());
                }

                // place the bid
                auction.placeBid(player, bid);
                handler.broadcast(auction, plugin.getMessage("messages.auctionFormattable.bid"), false);
                plugin.getEconomy().withdrawPlayer(player, bid);
            }
        }

        return true;
    }
}

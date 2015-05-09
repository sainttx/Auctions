package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 09/05/2015.
 */
public class BidCommand extends AuctionSubCommand {

    public BidCommand() {
        super("auctions.bid", "bid", "b");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        Auction auction = manager.getCurrentAuction();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can place bids on auctions");
        } else if (args.length < 2 && !plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true)) {
            manager.getMessageHandler().sendMessage("fail-bid-syntax", sender);
        } else if (auction == null) {
            manager.getMessageHandler().sendMessage("fail-bid-no-auction", sender);
        } else {
            Player player = (Player) sender;

            double bid;

            try {
                bid = args.length < 2
                        ? auction.getTopBid() + auction.getBidIncrement()
                        : Double.parseDouble(args[1]);
            } catch (NumberFormatException ex) {
                manager.getMessageHandler().sendMessage("fail-bid-number", sender);
                return true;
            }

            if (plugin.getConfig().isList("general.disabledWorlds")
                    && plugin.getConfig().getStringList("general.disabledWorlds").contains(player.getWorld().getName())) {
                manager.getMessageHandler().sendMessage("fail-start-world-disabled", player);
            } else if (manager.getMessageHandler().isIgnoring(player.getUniqueId())) {
                manager.getMessageHandler().sendMessage("fail-start-ignoring", player); // player is ignoring
            } else if (auction.getOwner().equals(player.getUniqueId())) {
                manager.getMessageHandler().sendMessage("fail-bid-your-auction", player); // cant bid on own auction
            } else if (bid < auction.getTopBid() + auction.getBidIncrement()) {
                manager.getMessageHandler().sendMessage("fail-bid-too-low", player); // the bid wasnt enough
            } else if (plugin.getEconomy().getBalance(player) < bid) {
                manager.getMessageHandler().sendMessage("fail-bid-insufficient-balance", player); // insufficient funds
            } else if (player.getUniqueId().equals(auction.getTopBidder())) {
                manager.getMessageHandler().sendMessage("fail-bid-top-bidder", player); // already top bidder
            } else {
                if (auction.getTopBidder() != null) { // give the old winner their money back
                    OfflinePlayer oldPlayer = Bukkit.getOfflinePlayer(auction.getTopBidder());
                    plugin.getEconomy().depositPlayer(oldPlayer, auction.getTopBid());
                }

                // place the bid
                manager.getMessageHandler().broadcast(auction, "bid-broadcast", false);
                auction.placeBid(player, bid);
                plugin.getEconomy().withdrawPlayer(player, bid);
            }
        }

        return true;
    }
}

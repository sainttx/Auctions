package me.sainttx.auction.command;

import me.sainttx.auction.Auction;
import me.sainttx.auction.AuctionManager;
import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.Messages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBid implements CommandExecutor {

    private AuctionPlugin pl;

    public CommandBid(AuctionPlugin pl) {
        this.pl = pl;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        Messages m = Messages.getMessager();

        if (!sender.hasPermission("auction.bid")) {
            m.sendText(sender, "insufficient-permissions", true);
        }

        else if (args.length == 0 && pl.isAllowAutobid()) {
            Auction auction = AuctionManager.getCurrentAuction();

            if (auction != null) {
                pl.manager.prepareBid(player, (int) (auction.getTopBid() + pl.getMinBidIncrement()));
            } else { 
                m.sendText(sender, "fail-bid-no-auction", true);
            }
        } 

        else if (args.length == 1) {
            pl.manager.prepareBid(player, args[0]);
        }

        else {
            m.sendText(sender, "fail-bid-syntax", true);
        }

        return false;
    }
}

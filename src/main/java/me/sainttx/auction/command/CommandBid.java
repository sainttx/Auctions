package me.sainttx.auction.command;

import me.sainttx.auction.Auction;
import me.sainttx.auction.AuctionManager;
import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.TextUtil;

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
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command!");
            return false;
        }

        Player player = (Player) sender;

        if (!sender.hasPermission("auction.bid")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("insufficient-permissions"), player);
        } else if (args.length == 0 && pl.isAllowAutobid()) {
            Auction auction = AuctionManager.getCurrentAuction();

            if (auction != null) {
                pl.manager.prepareBid(player, (int) (auction.getTopBid() + pl.getMinBidIncrement()));
            } else {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-no-auction"), player);
            }
        } else if (args.length == 1) {
            pl.manager.prepareBid(player, args[0]);
        } else {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-syntax"), player);
        }

        return false;
    }
}

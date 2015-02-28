package me.sainttx.auction.command;

import me.sainttx.auction.Auction;
import me.sainttx.auction.AuctionManager;
import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.util.TextUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BidCommand implements CommandExecutor {

    /*
     * The Auction plugin
     */
    private AuctionPlugin plugin;

    /**
     * Create the bid command controller
     */
    public BidCommand(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this command!");
            return false;
        }

        Player player = (Player) sender;

        if (!sender.hasPermission("auction.bid")) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("insufficient-permissions"), true, player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-ignoring"), true, player);
        } else if (args.length == 0 && plugin.getConfig().getBoolean("allow-using-bid-auto-command", true)) {
            Auction auction = AuctionManager.getCurrentAuction();

            if (auction != null) {
                AuctionManager.getAuctionManager().prepareBid(player, auction.getTopBid() + auction.getBidIncrement());
            } else {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-no-auction"), true, player);
            }
        } else if (args.length == 1) {
            AuctionManager.getAuctionManager().prepareBid(player, args[0]);
        } else {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-syntax"), true, player);
        }

        return false;
    }
}

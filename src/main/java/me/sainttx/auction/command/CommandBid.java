package me.sainttx.auction.command;

import me.sainttx.auction.Auction;
import me.sainttx.auction.AuctionManager;
import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.util.TextUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBid implements CommandExecutor {

    /*
     * The Auction plugin
     */
    private AuctionPlugin plugin;

    /**
     * Create the bid command controller
     */
    public CommandBid(AuctionPlugin plugin) {
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
            TextUtil.sendMessage(TextUtil.getConfigMessage("insufficient-permissions"), player);
        } else if (args.length == 0 && plugin.getConfig().getBoolean("allow-autobid", false)) {
            Auction auction = AuctionManager.getCurrentAuction();

            if (auction != null) {
                AuctionManager.getAuctionManager().prepareBid(player, (int) (auction.getTopBid() + plugin.getConfig().getDouble("minimum-bid-increment", 1D)));
            } else {
                TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-no-auction"), player);
            }
        } else if (args.length == 1) {
            AuctionManager.getAuctionManager().prepareBid(player, args[0]);
        } else {
            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-syntax"), player);
        }

        return false;
    }
}

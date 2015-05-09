package com.sainttx.auction.command;

import com.sainttx.auction.AuctionManagerImpl;
import com.sainttx.auction.AuctionBlah;
import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.util.TextUtil;

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
            plugin.getMessageHandler().sendMessage("insufficient-permissions", player);
        } else if (TextUtil.isIgnoring(player.getUniqueId())) {
            plugin.getMessageHandler().sendMessage("fail-start-ignoring", player);
        } else if (plugin.getConfig().isList("general.disabledWorlds")
                && plugin.getConfig().getStringList("general.disabledWorlds").contains(player.getWorld().getName())) {
            plugin.getMessageHandler().sendMessage("fail-start-world-disabled", player);
        } else if (args.length == 0 && plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true)) {
            AuctionBlah auction = AuctionManagerImpl.getCurrentAuction();

            if (auction != null) {
                AuctionManagerImpl.getAuctionManager().prepareBid(player, auction.getTopBid() + auction.getBidIncrement());
            } else {
                plugin.getMessageHandler().sendMessage("fail-bid-no-auction", player);
            }
        } else if (args.length == 1) {
            AuctionManagerImpl.getAuctionManager().prepareBid(player, args[0]);
        } else {
            plugin.getMessageHandler().sendMessage("fail-bid-syntax", player);
        }

        return false;
    }
}

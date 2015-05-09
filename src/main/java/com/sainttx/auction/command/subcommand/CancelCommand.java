package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 09/05/2015.
 */
public class CancelCommand extends AuctionSubCommand {

    public CancelCommand() {
        super("auctions.cancel", "cancel", "c", "can");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (manager.getCurrentAuction() == null) {
            // No auction
            manager.getMessageHandler().sendMessage("fail-cancel-no-auction", sender);
        } else if (sender instanceof Player && manager.getMessageHandler().isIgnoring(((Player) sender).getUniqueId())) {
            // Ignoring
            manager.getMessageHandler().sendMessage("fail-start-ignoring", sender);
        } else if (!sender.hasPermission("auction.cancel.bypass")
                && !plugin.getConfig().getBoolean("allow-auction-cancel-command", true)) {
            // Can't cancel
            manager.getMessageHandler().sendMessage("fail-cancel-disabled", sender);
        } else if (manager.getCurrentAuction().getTimeLeft() < plugin.getConfig().getInt("auctionSettings.mustCancelBefore", 15)
                && !sender.hasPermission("auction.cancel.bypass")) {
            // Can't cancel
            manager.getMessageHandler().sendMessage("fail-cancel-time", sender);
        } else if (sender instanceof Player
                && !manager.getCurrentAuction().getOwner().equals(((Player) sender).getUniqueId())
                && !sender.hasPermission("auction.cancel.bypass")) {
            // Can't cancel other peoples auction
            manager.getMessageHandler().sendMessage("fail-cancel-not-yours", sender);
        } else {
            manager.getMessageHandler().sendMessage(manager.getCurrentAuction(), "auction-cancelled", false);
            manager.getCurrentAuction().cancel();
        }
        return false;
    }
}

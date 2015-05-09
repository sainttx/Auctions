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
public class EndCommand extends AuctionSubCommand {

    public EndCommand() {
        super("auctions.end", "end", "e");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (manager.getCurrentAuction() == null) {
            manager.getMessageHandler().sendMessage("fail-end-no-auction", sender);
        } else if (!plugin.getConfig().getBoolean("allow-auction-end-command", false)
                && !sender.hasPermission("auction.end.bypass")) {
            manager.getMessageHandler().sendMessage("fail-end-disallowed", sender);
        } else if (!sender.hasPermission("auction.end.bypass")
                && sender instanceof Player
                && !manager.getCurrentAuction().getOwner().equals(((Player) sender).getUniqueId())) {
            manager.getMessageHandler().sendMessage("fail-end-not-your-auction", sender);
        } else {
            manager.getCurrentAuction().end(true);
            manager.setCurrentAuction(null);
        }
        return false;
    }
}

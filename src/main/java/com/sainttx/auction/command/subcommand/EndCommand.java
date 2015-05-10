package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction end command for the auction plugin
 */
public class EndCommand extends AuctionSubCommand {

    public EndCommand() {
        super("auctions.end", "end", "e");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (manager.getCurrentAuction() == null) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (!sender.hasPermission("auction.end.bypass")
                && sender instanceof Player
                && !manager.getCurrentAuction().getOwner().equals(((Player) sender).getUniqueId())) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.notYourAuction"));
        } else {
            manager.getCurrentAuction().end(true);
            manager.setCurrentAuction(null);
        }
        return false;
    }
}

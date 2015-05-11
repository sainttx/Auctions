package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction end command for the auction plugin
 */
public class EndCommand extends AuctionSubCommand {

    public EndCommand() {
        super("auctions.command.end", "end", "e");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (manager.getCurrentAuction() == null) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else if (!sender.hasPermission("auctions.bypass.end.otherauctions")
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

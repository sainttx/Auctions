package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Handles the /auction info command for the auction plugin
 */
public class InfoCommand extends AuctionSubCommand {

    public InfoCommand() {
        super("auctions.info", "info", "i");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (manager.getCurrentAuction() == null) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else {
            manager.getMessageHandler().sendAuctionInformation(sender, manager.getCurrentAuction());
        }
        return false;
    }
}

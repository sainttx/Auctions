package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Handles the /auction info command for the auction plugin
 */
public class InfoCommand extends AuctionSubCommand {

    public InfoCommand() {
        super("auctions.command.info", "info", "i");
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

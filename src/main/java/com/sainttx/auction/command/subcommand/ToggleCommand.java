package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Handles the /auction toggle command for the auction plugin
 */
public class ToggleCommand extends AuctionSubCommand {

    public ToggleCommand() {
        super("auctions.toggle", "toggle", "t");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        manager.setAuctioningDisabled(!manager.isAuctioningDisabled());
        String message = manager.isAuctioningDisabled() ? "messages.auctionsDisabled" : "messages.auctionsEnabled";
        manager.getMessageHandler().broadcast(plugin.getMessage(message), true);
        return false;
    }
}

package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Handles the /auction toggle command for the auction plugin
 */
public class ToggleCommand extends AuctionSubCommand {

    public ToggleCommand() {
        super("auctions.command.toggle", "toggle", "t");
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

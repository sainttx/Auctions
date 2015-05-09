package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Created by Matthew on 09/05/2015.
 */
public class InfoCommand extends AuctionSubCommand {

    public InfoCommand() {
        super("auctions.info", "info", "i");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();

        if (manager.getCurrentAuction() == null) {
            manager.getMessageHandler().sendMessage("fail-info-no-auction", sender);
        } else {
            manager.getMessageHandler().sendAuctionInformation(sender, manager.getCurrentAuction());
        }
        return false;
    }
}

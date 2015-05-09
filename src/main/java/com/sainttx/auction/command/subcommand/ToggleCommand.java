package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import com.sainttx.auction.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Created by Matthew on 09/05/2015.
 */
public class ToggleCommand extends AuctionSubCommand {

    public ToggleCommand() {
        super("auctions.toggle", "toggle", "t");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        manager.setAuctioningDisabled(!manager.isAuctioningDisabled());
        String message = manager.isAuctioningDisabled() ? "broadcast-disable" : "broadcast-enable";
        Bukkit.broadcastMessage(TextUtil.getConfigMessage(message));
        return false;
    }
}

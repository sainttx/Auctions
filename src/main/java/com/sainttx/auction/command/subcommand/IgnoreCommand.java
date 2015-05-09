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
public class IgnoreCommand extends AuctionSubCommand {

    public IgnoreCommand() {
        super("auctions.ignore", "ignore", "i");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this sub command");
        } else {
            AuctionManager manager = AuctionsAPI.getAuctionManager();
            Player player = (Player) sender;

            if (manager.getMessageHandler().isIgnoring(player.getUniqueId())) {
                manager.getMessageHandler().removeIgnoring(player.getUniqueId());
                manager.getMessageHandler().sendMessage("ignoring-off", player);
            } else {
                manager.getMessageHandler().addIgnoring(player.getUniqueId());
                manager.getMessageHandler().sendMessage("ignoring-on", player);
            }
        }
        return false;
    }
}

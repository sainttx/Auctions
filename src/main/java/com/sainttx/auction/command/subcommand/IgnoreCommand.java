package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction ignore command for the auction plugin
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
            MessageHandler handler = manager.getMessageHandler();
            Player player = (Player) sender;

            if (manager.getMessageHandler().isIgnoring(player.getUniqueId())) {
                handler.removeIgnoring(player.getUniqueId());
                handler.sendMessage(plugin.getMessage("messages.noLongerIgnoring"), player);
            } else {
                handler.addIgnoring(player.getUniqueId());
                handler.sendMessage(plugin.getMessage("messages.nowIgnoring"), player);
            }
        }
        return false;
    }
}

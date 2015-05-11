package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /auction ignore command for the auction plugin
 */
public class IgnoreCommand extends AuctionSubCommand {

    public IgnoreCommand() {
        super("auctions.command.ignore", "ignore", "i");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can ignore the plugin");
        } else {
            AuctionManager manager = AuctionsAPI.getAuctionManager();
            MessageHandler handler = manager.getMessageHandler();
            Player player = (Player) sender;

            if (manager.getMessageHandler().isIgnoring(player)) {
                handler.removeIgnoring(player);
                handler.sendMessage(player, plugin.getMessage("messages.noLongerIgnoring"));
            } else {
                handler.addIgnoring(player);
                handler.sendMessage(player, plugin.getMessage("messages.nowIgnoring"));
            }
        }
        return false;
    }
}

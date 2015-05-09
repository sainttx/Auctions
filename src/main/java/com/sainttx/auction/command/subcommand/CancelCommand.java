package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.api.messages.MessageHandler;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 09/05/2015.
 */
public class CancelCommand extends AuctionSubCommand {

    public CancelCommand() {
        super("auctions.cancel", "cancel", "c", "can");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();

        if (manager.getCurrentAuction() == null) {
            // No auction
            handler.sendMessage(plugin.getMessage("messages.error.noCurrentAuction"), sender);
        } else if (sender instanceof Player && manager.getMessageHandler().isIgnoring(((Player) sender).getUniqueId())) {
            // Ignoring
            handler.sendMessage(plugin.getMessage("messages.error.currentlyIgnoring"), sender);
        } else if (manager.getCurrentAuction().getTimeLeft() < plugin.getConfig().getInt("auctionSettings.mustCancelBefore", 15)
                && !sender.hasPermission("auction.cancel.bypass")) {
            // Can't cancel
            handler.sendMessage(plugin.getMessage("messages.error.cantCancelNow"), sender);
        } else if (sender instanceof Player
                && !manager.getCurrentAuction().getOwner().equals(((Player) sender).getUniqueId())
                && !sender.hasPermission("auction.cancel.bypass")) {
            // Can't cancel other peoples auction
            handler.sendMessage(plugin.getMessage("messages.error.notYourAuction"), sender);
        } else {
            manager.getCurrentAuction().cancel();
        }
        return false;
    }
}

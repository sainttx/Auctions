package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.reward.Reward;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 09/05/2015.
 */
public class ImpoundCommand extends AuctionSubCommand {

    public ImpoundCommand() {
        super("auctions.command.impound", "impound");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        MessageHandler handler = manager.getMessageHandler();
        Auction auction = manager.getCurrentAuction();

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can impound auctions");
        } else if (auction == null) {
            handler.sendMessage(sender, plugin.getMessage("messages.error.noCurrentAuction"));
        } else {
            Player player = (Player) sender;
            Reward reward = auction.getReward();
            auction.impound();
            reward.giveItem(player);

            String message = plugin.getMessage("messages.auctionImpounded")
                    .replace("[player]", player.getName());
            handler.broadcast(message, false);
        }

        return true;
    }
}

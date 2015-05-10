package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.api.Auction;
import com.sainttx.auction.api.AuctionManager;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Queue;

/**
 * Created by Matthew on 10/05/2015.
 */
public class QueueCommand extends AuctionSubCommand {

    public QueueCommand() {
        super("auctions.queue", "queue", "q");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        Queue<Auction> queue = manager.getQueue();

        if (queue.isEmpty()) {
            manager.getMessageHandler().sendMessage(plugin.getMessage("messages.error.noAuctionsInQueue"), sender);
        } else {
            int queuePosition = 1;

            manager.getMessageHandler().sendMessage(plugin.getMessage("messages.queueInfoHeader"), sender);
            for (Auction auction : queue) {
                String message = plugin.getMessage("messages.auctionFormattable.queueInfoLine")
                        .replace("[queuepos]", Integer.toString(queuePosition));
                manager.getMessageHandler().sendMessage(auction, message, sender);
                queuePosition++;
            }
        }

        return true;
    }
}

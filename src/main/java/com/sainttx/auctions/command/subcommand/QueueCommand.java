package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionManager;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Queue;

/**
 * Created by Matthew on 10/05/2015.
 */
public class QueueCommand extends AuctionSubCommand {

    public QueueCommand() {
        super("auctions.command.queue", "queue", "q");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionManager manager = AuctionsAPI.getAuctionManager();
        Queue<Auction> queue = manager.getQueue();

        if (queue.isEmpty()) {
            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.error.noAuctionsInQueue"));
        } else {
            int queuePosition = 1;

            manager.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.queueInfoHeader"));
            for (Auction auction : queue) {
                String message = plugin.getMessage("messages.auctionFormattable.queueInfoLine")
                        .replace("[queuepos]", Integer.toString(queuePosition));
                manager.getMessageHandler().sendMessage(sender, message, auction);
                queuePosition++;
            }
        }

        return true;
    }
}

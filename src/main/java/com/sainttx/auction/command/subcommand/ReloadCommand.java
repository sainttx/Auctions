package com.sainttx.auction.command.subcommand;

import com.sainttx.auction.command.AuctionSubCommand;
import com.sainttx.auction.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Created by Matthew on 09/05/2015.
 */
public class ReloadCommand extends AuctionSubCommand {

    public ReloadCommand() {
        super("auctions.reload", "reload", "r", "rel");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(TextUtil.getConfigMessage("reload"));
        plugin.reloadConfig();
        plugin.loadConfig();
        TextUtil.load(plugin);
        return false;
    }
}

package com.sainttx.auctions.command.subcommand;

import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.command.AuctionSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Handles the /auction reload command for the auction plugin
 */
public class ReloadCommand extends AuctionSubCommand {

    public ReloadCommand() {
        super("auctions.command.reload", "reload", "r", "rel");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        AuctionsAPI.getMessageHandler().sendMessage(sender, plugin.getMessage("messages.pluginReloaded"));
        plugin.reloadConfig();
        plugin.loadConfig();
        return false;
    }
}

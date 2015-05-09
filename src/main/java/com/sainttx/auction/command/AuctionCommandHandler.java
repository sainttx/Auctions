package com.sainttx.auction.command;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.subcommand.BidCommand;
import com.sainttx.auction.command.subcommand.*;
import com.sainttx.auction.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;

public class AuctionCommandHandler implements CommandExecutor {

    private Set<AuctionSubCommand> commands = new HashSet<AuctionSubCommand>();

    public AuctionCommandHandler() {
        commands.add(new BidCommand());
        commands.add(new CancelCommand());
        commands.add(new EndCommand());
        commands.add(new IgnoreCommand());
        commands.add(new InfoCommand());
        commands.add(new ReloadCommand());
        commands.add(new StartCommand());
        commands.add(new ToggleCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && !command.getName().equalsIgnoreCase("bid")) {
            TextUtil.sendMenu(sender);
        } else {
            AuctionPlugin plugin = AuctionPlugin.getPlugin();
            String sub = command.getName().equalsIgnoreCase("bid") ? "bid" : args[0];

            for (AuctionSubCommand cmd : commands) {
                if (cmd.canTrigger(sub)) {
                    if (!sender.hasPermission(cmd.getPermission())) {
                        AuctionsAPI.getMessageHandler().sendMessage(
                                plugin.getMessage("messages.error.insufficientPermissions"), sender);
                    } else {
                        cmd.onCommand(sender, command, label, args);
                    }
                    return true;
                }
            }

            TextUtil.sendMenu(sender);
        }
        return true;
    }
}

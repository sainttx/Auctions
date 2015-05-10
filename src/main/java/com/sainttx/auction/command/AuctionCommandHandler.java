package com.sainttx.auction.command;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.command.subcommand.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles command distribution for the auction plugin
 */
public class AuctionCommandHandler implements CommandExecutor {

    /*
     * All commands for the plugin
     */
    private Set<AuctionSubCommand> commands = new HashSet<AuctionSubCommand>();

    /**
     * Constructor. Initializes all subcommands.
     */
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
            sendMenu(sender);
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

            sendMenu(sender);
        }
        return true;
    }

    /**
     * Sends the auction menu to a sender
     *
     * @param sender The sender to send the menu too
     */
    public void sendMenu(CommandSender sender) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();

        for (String message : plugin.getConfig().getStringList("messages.helpMenu")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}

package me.sainttx.auction.command;

import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.Messages;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandAuction implements CommandExecutor {

    private AuctionPlugin pl;

    public CommandAuction(AuctionPlugin pl) {
        this.pl = pl;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = Messages.getMessager();

        if (args.length == 0) {
            m.sendMenu(sender);
        } else {
            String subCommand = args[0].toLowerCase();

            if (!sender.hasPermission("auction." + subCommand)) {
                m.sendText(sender, "insufficient-permissions", true);
                return false;
            }

            if (subCommand.equals("reload")) 
            {
                m.sendText(sender, "reload", true);
                pl.reloadConfig();
                pl.loadConfig();
                m.reload();
            } 

            else if (subCommand.equals("toggle")) 
            {
                pl.manager.setDisabled(!pl.manager.isDisabled());
                pl.getServer().broadcastMessage(pl.manager.isDisabled() 
                        ? m.getMessageFile().getString("broadcast-disable")
                                : m.getMessageFile().getString("broadcast-enable"));
            }

            else if (Player.class.isAssignableFrom(sender.getClass())) {
                Player player = (Player) sender;

                if (subCommand.equals("start")) {
                    if (m.isIgnoring(sender.getName())) {
                        m.sendText(sender, "fail-start-ignoring", true);
                    } else if (player.getGameMode() == GameMode.CREATIVE && !pl.isAllowCreative() && !player.hasPermission("auction.creative")) {
                        m.sendText(sender, "fail-start-creative", true);
                    } else {
                        pl.manager.prepareAuction(player, args);
                    }
                }

                else if (subCommand.equals("bid")) {
                    if (args.length == 2) {
                        pl.manager.prepareBid(player, args[1]); 
                    } else {
                        m.sendText(sender, "fail-bid-syntax", true);
                    }
                } 

                else if (subCommand.equals("info")) {
                    pl.manager.sendAuctionInfo(player);
                } 

                else if (subCommand.equals("end")) {
                    pl.manager.end(player);
                } 

                else if (subCommand.equals("ignore") || subCommand.equals("quiet")) {
                    if (!m.isIgnoring(sender.getName())) {
                        m.addIgnoring(sender.getName());
                        m.sendText(sender, "ignoring-on", true);
                    } else {
                        m.removeIgnoring(sender.getName());
                        m.sendText(sender, "ignoring-off", true);
                    }
                }
            } 
            
            else {
                m.sendMenu(sender);
            }
        }
        return false;
    }
}

package me.sainttx.auction.command;

import me.sainttx.auction.AuctionManager;
import me.sainttx.auction.AuctionPlugin;
import me.sainttx.auction.util.TextUtil;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuctionCommand implements CommandExecutor {

    /*
     * The Auction plugin
     */
    private AuctionPlugin plugin;

    /**
     * Create the auction command controller
     */
    public AuctionCommand(AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            TextUtil.sendMenu(sender);
        } else {
            String subCommand = args[0].toLowerCase();
            AuctionManager manager = AuctionManager.getAuctionManager();

            if (!sender.hasPermission("auction." + subCommand)) {
                sender.sendMessage(TextUtil.getConfigMessage("insufficient-permissions"));
            } else if (subCommand.equals("reload")) {
                sender.sendMessage(TextUtil.getConfigMessage("reload"));
                plugin.reloadConfig();
                plugin.loadConfig();
                TextUtil.load(plugin);
            } else if (subCommand.equals("toggle")) {
                manager.setDisabled(!manager.isDisabled());
                plugin.getServer().broadcastMessage(manager.isDisabled()
                        ? TextUtil.getConfigMessage("broadcast-disable")
                                : TextUtil.getConfigMessage("broadcast-enable"));
            } else if (sender instanceof Player) {
                Player player = (Player) sender;

                if (subCommand.equals("ignore")) {
                    if (!TextUtil.isIgnoring(player.getUniqueId())) {
                        TextUtil.addIgnoring(player.getUniqueId());
                        TextUtil.sendMessage(TextUtil.getConfigMessage("ignoring-on"), true, player);
                    } else {
                        TextUtil.removeIgnoring(player.getUniqueId());
                        TextUtil.sendMessage(TextUtil.getConfigMessage("ignoring-off"), true, player);
                    }
                } else if (TextUtil.isIgnoring(player.getUniqueId())) {
                    TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-ignoring"), true, player);
                } else if (subCommand.equals("start")) {
                    if (player.getGameMode() == GameMode.CREATIVE && !plugin.getConfig().getBoolean("allow-creative", false) && !player.hasPermission("auction.creative")) {
                        TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-creative"), true, player);
                    } else {
                        manager.prepareAuction(player, args);
                    }
                } else if (subCommand.equals("bid")) {
                    if (args.length == 2) {
                        manager.prepareBid(player, args[1]);
                    } else {
                        TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-syntax"), true, player);
                    }
                } else if (subCommand.equals("info")) {
                    manager.sendAuctionInfo(player);
                } else if (subCommand.equals("end")) {
                    manager.end(player);
                } else if (subCommand.equals("help")) {
                    if (plugin.getConfig().getBoolean("allow-help-command", false)) {
                        TextUtil.sendHelp(sender);
                    } else {
                        TextUtil.sendMenu(sender);
                    }
                } else {
                    TextUtil.sendMenu(sender);
                }
            } else {
                TextUtil.sendMenu(sender);
            }
        }
        return false;
    }
}

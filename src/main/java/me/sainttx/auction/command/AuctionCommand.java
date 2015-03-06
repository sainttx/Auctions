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
            AuctionManager manager = AuctionManager.getAuctionManager();
            SubCommand subCommand = SubCommand.getSubCommand(args[0]);

            if (subCommand == null) {
                TextUtil.sendMenu(sender);
            } else if (!sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage(TextUtil.getConfigMessage("insufficient-permissions"));
            } else if (!(sender instanceof Player) && subCommand.isPlayerCommand()) {
                sender.sendMessage("You must be a player to use this sub-command!");
            } else {
                Player player = null;

                try {
                    player = (Player) sender; // Attempt to cast

                    if (plugin.getConfig().isList("disabled-worlds")
                            && plugin.getConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
                        TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-world-disabled"), true, player);
                        return true;
                    }
                } catch (ClassCastException ignored) { /* Do nothing */ }

                switch (subCommand) {
                    case RELOAD:
                        sender.sendMessage(TextUtil.getConfigMessage("reload"));
                        plugin.reloadConfig();
                        plugin.loadConfig();
                        TextUtil.load(plugin);
                        break;
                    case TOGGLE:
                        manager.setDisabled(!manager.isDisabled());
                        plugin.getServer().broadcastMessage(manager.isDisabled()
                                ? TextUtil.getConfigMessage("broadcast-disable")
                                : TextUtil.getConfigMessage("broadcast-enable"));
                        break;
                    case IGNORE:
                        if (!TextUtil.isIgnoring(player.getUniqueId())) {
                            TextUtil.addIgnoring(player.getUniqueId());
                            TextUtil.sendMessage(TextUtil.getConfigMessage("ignoring-on"), true, player);
                        } else {
                            TextUtil.removeIgnoring(player.getUniqueId());
                            TextUtil.sendMessage(TextUtil.getConfigMessage("ignoring-off"), true, player);
                        }
                        break;
                    case START:
                        if (player.getGameMode() == GameMode.CREATIVE && !plugin.getConfig().getBoolean("allow-creative-auctioning", false) && !player.hasPermission("auction.creative")) {
                            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-start-creative"), true, player);
                        } else {
                            manager.prepareAuction(player, args);
                        }
                        break;
                    case BID:
                        if (args.length == 2) {
                            manager.prepareBid(player, args[1]);
                        } else {
                            TextUtil.sendMessage(TextUtil.getConfigMessage("fail-bid-syntax"), true, player);
                        }
                        break;
                    case INFO:
                        manager.sendAuctionInfo(player);
                        break;
                    case END:
                        manager.end(player);
                        break;
                    case HELP:
                        if (plugin.getConfig().getBoolean("allow-auction-help-command", false)) {
                            TextUtil.sendHelp(sender);
                        } else {
                            TextUtil.sendMenu(sender);
                        }
                        break;
                    case CANCEL:
                        manager.cancelCurrentAuction(player);
                        break;
                }
            }
        }

        return true;
    }

    /**
     * An enumerate containing the possible SubCommands
     * that can be executed by the Auction plugin
     */
    private enum SubCommand {
        BID("auction.bid", true, "bid", "b"),
        CANCEL("auction.cancel", false, "cancel", "c"),
        END("auction.end", false, "end", "e"),
        HELP("auction.help", false, "help"),
        IGNORE("auction.ignore", true, "ignore", "quiet"),
        INFO("auction.info", true, "info", "i"),
        RELOAD("auction.reload", false, "reload"),
        START("auction.start", true, "start", "s"),
        TOGGLE("auction.toggle", false, "toggle");

        /*
         * The permission node required
         */
        private String permission;

        /*
         * Allowed aliases for this sub-command
         */
        private String[] aliases;

        /*
         * Whether or not the sender must be a player to use this command
         */
        private boolean isPlayerCommand;

        /**
         * Create
         */
        SubCommand(String permission, boolean isPlayerCommand, String... aliases) {
            this.permission = permission;
            this.aliases = aliases;
            this.isPlayerCommand = isPlayerCommand;
        }

        /**
         * Returns the permission required to use the SubCommand
         *
         * @return The permission node required
         */
        public String getPermission() {
            return permission;
        }

        /**
         * Returns whether or not the command must be ran by a Player
         *
         * @return True if the command must be ran by a Player, false otherwise
         */
        public boolean isPlayerCommand() {
            return isPlayerCommand;
        }

        /**
         * Gets a SubCommand from a players entry
         *
         * @param entry The command a player typed
         * @return The corresponding SubCommand
         */
        public static SubCommand getSubCommand(String entry) {
            for (SubCommand sc : values()) {
                for (String alias : sc.aliases) {
                    if (alias.equalsIgnoreCase(entry)) {
                        return sc;
                    }
                }
            }

            return null;
        }
    }
}

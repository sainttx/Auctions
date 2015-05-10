package com.sainttx.auction.command;

import com.sainttx.auction.AuctionPlugin;
import org.bukkit.command.CommandExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A template for sub-commands in the auction plugin
 */
public abstract class AuctionSubCommand implements CommandExecutor {

    protected AuctionPlugin plugin = AuctionPlugin.getPlugin();
    private Set<String> aliases = new HashSet<String>();
    private String permission;

    public AuctionSubCommand(String permission, String... aliases) {
        this.permission = permission;
        this.aliases.addAll(Arrays.asList(aliases));
    }

    /**
     * Gets the permission node associated with the subcommand
     *
     * @return the permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets whether an alias will trigger this sub command
     *
     * @param alias the string alias
     * @return true if the alias correlates to this sub command
     */
    public boolean canTrigger(String alias) {
        return aliases.contains(alias.toLowerCase());
    }
}

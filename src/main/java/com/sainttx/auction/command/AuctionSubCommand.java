package com.sainttx.auction.command;

import com.sainttx.auction.AuctionPlugin;
import org.bukkit.command.CommandExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Matthew on 09/05/2015.
 */
public abstract class AuctionSubCommand implements CommandExecutor {

    protected AuctionPlugin plugin = AuctionPlugin.getPlugin();
    private Set<String> aliases = new HashSet<String>();
    private String permission;

    public AuctionSubCommand(String permission, String... aliases) {
        this.permission = permission;
        this.aliases.addAll(Arrays.asList(aliases));
    }

    public String getPermission() {
        return permission;
    }

    public boolean canTrigger(String alias) {
        return aliases.contains(alias.toLowerCase());
    }
}

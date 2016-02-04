/*
 * Copyright (C) SainttX <http://sainttx.com>
 * Copyright (C) contributors
 *
 * This file is part of Auctions.
 *
 * Auctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Auctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Auctions.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sainttx.auctions.command;

import com.sainttx.auctions.AuctionPluginImpl;
import org.bukkit.command.CommandExecutor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A template for sub-commands in the auction plugin
 */
public abstract class AuctionSubCommand implements CommandExecutor {

    protected final AuctionPluginImpl plugin;
    private Set<String> aliases = new HashSet<>();
    private String permission;

    public AuctionSubCommand(AuctionPluginImpl plugin, String permission, String... aliases) {
        this.plugin = plugin;
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

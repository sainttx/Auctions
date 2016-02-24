package com.sainttx.auctions.api.messages;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public class WorldPlayersGroup implements MessageGroup {

    private final World world;

    public WorldPlayersGroup(String world) {
        this(Bukkit.getWorld(world));
    }

    public WorldPlayersGroup(World world) {
        Validate.notNull(world, "World cannot be null");
        this.world = world;
    }

    @Override
    public Collection<? extends CommandSender> getRecipients() {
        return world.getPlayers();
    }
}

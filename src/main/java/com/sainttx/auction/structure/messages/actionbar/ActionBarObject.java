package com.sainttx.auction.structure.messages.actionbar;

import org.bukkit.entity.Player;

/**
 * Created by Matthew on 10/05/2015.
 */
public interface ActionBarObject {

    /**
     * Sends the action bar to a player
     *
     * @param player the player to send the action bar too
     */
    void send(Player player);
}

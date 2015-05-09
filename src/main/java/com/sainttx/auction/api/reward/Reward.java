package com.sainttx.auction.api.reward;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

/**
 * Represents a reward that can be given as a result of an auction
 */
public interface Reward extends ConfigurationSerializable {

    /**
     * Gives the reward to the player
     *
     * @param player the player
     */
    void giveItem(Player player);

    /**
     * Returns the name or description of this reward for auction message formatting
     */
    String getName();

    /**
     * Returns the amount or multiplier of the reward
     *
     * @return the 'amount' present in the reward
     */
    int getAmount();
}

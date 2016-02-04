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

package com.sainttx.auctions.api.reward;

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

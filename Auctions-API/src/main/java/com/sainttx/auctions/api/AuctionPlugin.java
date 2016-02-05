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

package com.sainttx.auctions.api;

import com.sainttx.auctions.api.messages.MessageHandler;
import com.sainttx.auctions.api.reward.Reward;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public interface AuctionPlugin extends Plugin {

    /**
     * Returns the AuctionManager instance
     *
     * @return the manager instance
     */
    AuctionManager getManager();

    /**
     * Returns the current MessageHandler
     *
     * @return the handler instance
     */
    MessageHandler getMessageHandler();

    /**
     * Returns the Vault economy provider
     *
     * @return the vault economy provider
     */
    Economy getEconomy();

    /**
     * Returns the {@link Settings} for the plugin.
     *
     * @return the settings
     */
    Settings getSettings();

    /**
     * Gets a message from configuration
     *
     * @param path the path to the message
     * @return the message at the path
     */
    String getMessage(String path);

    /**
     * Gets an items name
     *
     * @param item the item
     * @return the display name of the item
     */
    String getItemName(ItemStack item);

    /**
     * Saves a players auctioned reward to file if the plugin was unable
     * to return it
     *
     * @param uuid The ID of a player
     * @param reward The reward that was auctioned
     */
    void saveOfflinePlayer(UUID uuid, Reward reward);

    /**
     * Gets a stored reward for a UUID. Returns null if there is no reward for the id.
     *
     * @param uuid the uuid
     * @return the stored reward
     */
    Reward getOfflineReward(UUID uuid);

    /**
     * Removes a reward that is stored for a UUID
     *
     * @param uuid the uuid
     */
    void removeOfflineReward(UUID uuid);

    /**
     * Formats a double to english
     *
     * @param d the double
     * @return the english string representation
     */
    String formatDouble(double d);

}

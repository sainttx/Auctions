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

package com.sainttx.auctions;

import com.sainttx.auctions.api.AuctionPlugin;
import com.sainttx.auctions.api.Settings;
import com.sainttx.auctions.misc.DoubleConsts;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class LocalSettings implements Settings {

    private final AuctionPlugin plugin;

    LocalSettings(final AuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public double getStartFee() {
        return plugin.getConfig().getDouble("auctionSettings.startFee", 0D);
    }

    @Override
    public double getTaxPercent() {
        return plugin.getConfig().getDouble("auctionSettings.taxPercent", 0D);
    }

    @Override
    public int getStartTime() {
        return plugin.getConfig().getInt("auctionSettings.startTime", 30);
    }

    @Override
    public int getDelayBetweenAuctions() {
        return plugin.getConfig().getInt("auctionSettings.delayBetween", 5);
    }

    @Override
    public double getDefaultBidIncrement() {
        return plugin.getConfig().getDouble("auctionSettings.defaultBidIncrement", 50D);
    }

    @Override
    public double getMinimumBidIncrement() {
        return plugin.getConfig().getDouble("auctionSettings.minimumBidIncrement", 10D);
    }

    @Override
    public double getMaximumBidIncrement() {
        return plugin.getConfig().getDouble("auctionSettings.maximumBidIncrement", 9999D);
    }

    @Override
    public boolean canBidIncrementExceedStartPrice() {
        return plugin.getConfig().getBoolean("auctionSettings.incrementCanExceedStartPrice", true);
    }

    @Override
    public double getMinimumStartPrice() {
        return plugin.getConfig().getDouble("auctionSettings.minimumStartPrice", 0D);
    }

    @Override
    public double getMaximumStartPrice() {
        return plugin.getConfig().getDouble("auctionSettings.maximumStartPrice", 99999);
    }

    @Override
    public double getMaximumAutowin() {
        return plugin.getConfig().getDouble("auctionSettings.maximumAutowinAmount", DoubleConsts.MILLION);
    }

    @Override
    public boolean canAuctionNamedItems() {
        return plugin.getConfig().getBoolean("auctionSettings.canAuctionNamedItems", true);
    }

    @Override
    public boolean canAuctionDamagedItems() {
        return plugin.getConfig().getBoolean("auctionSettings.canAuctionDamagedItems", true);
    }

    @Override
    public boolean canSpecifyAutowin() {
        return plugin.getConfig().getBoolean("auctionSettings.canSpecifyAutowin", true);
    }

    @Override
    public boolean canAuctionInCreative() {
        return plugin.getConfig().getBoolean("auctionSettings.canAuctionInCreative", false);
    }

    @Override
    public boolean canBidAutomatically() {
        return plugin.getConfig().getBoolean("auctionSettings.canBidAutomatically", true);
    }

    @Override
    public int getAuctionQueueLimit() {
        return plugin.getConfig().getInt("auctionSettings.auctionQueueLimit", 3);
    }

    @Override
    public int getMustCancelBeforeTime() {
        return plugin.getConfig().getInt("auctionSettings.mustCancelBefore", 15);
    }

    @Override
    public int getMustCancelAfterTime() {
        return plugin.getConfig().getInt("auctionSettings.mustCancelAfter", -1);
    }

    @Override
    public boolean isAntiSnipeEnabled() {
        return plugin.getConfig().getBoolean("auctionSettings.antiSnipe.enable", true);
    }

    @Override
    public int getAntiSnipeTimeThreshold() {
        return plugin.getConfig().getInt("auctionSettings.antiSnipe.timeThreshold", 3);
    }

    @Override
    public int getMaximumAntiSnipesPerAuction() {
        return plugin.getConfig().getInt("auctionSettings.antiSnipe.maxPerAuction", 3);
    }

    @Override
    public int getAntiSnipeExtraTime() {
        return plugin.getConfig().getInt("auctionSettings.antiSnipe.addSeconds", 5);
    }

    @Override
    public boolean shouldRunPostAuctionCommands() {
        return plugin.getConfig().getBoolean("auctionSettings.commandsAfterAuction.enable", false);
    }

    @Override
    public boolean shouldRunPostAuctionCommandsOnlyIfSold() {
        return plugin.getConfig().getBoolean("auctionSettings.commandsAfterAuction.onlyIfSold", true);
    }

    @Override
    public List<String> getPostAuctionCommands() {
        if (!plugin.getConfig().isList("auctionSettings.commandsAfterAuction.commands")) {
            return Collections.emptyList();
        }
        return plugin.getConfig().getStringList("auctionSettings.commandsAfterAuction.commands");
    }

    @Override
    public int getOfflineRewardTickDelay() {
        return plugin.getConfig().getInt("general.offlineRewardTickDelay", 20);
    }

    @Override
    public boolean shouldUseShortenedTimes() {
        return plugin.getConfig().getBoolean("general.shortenedTimeFormat", false);
    }

    @Override
    public boolean shouldTruncateNumbers() {
        return plugin.getConfig().getBoolean("general.truncatedNumberFormat", false);
    }

    @Override
    public boolean shouldStripDisplayNameColor() {
        return plugin.getConfig().getBoolean("general.stripItemDisplayNameColor", false);
    }

    @Override
    public boolean isDisabledWorld(String world) {
        return getDisabledWorlds().contains(world);
    }

    private Collection<String> getDisabledWorlds() {
        return new TreeSet<>(plugin.getConfig().getStringList("general.disabledWorlds"));
    }

    @Override
    public boolean shouldBlockCommandsIfAuctioning() {
        return plugin.getConfig().getBoolean("general.blockCommands.ifAuctioning", false);
    }

    @Override
    public boolean shouldBlockCommandsIfQueued() {
        return plugin.getConfig().getBoolean("general.blockCommands.ifQueued", false);
    }

    @Override
    public boolean shouldBlockCommandsIfTopBidder() {
        return plugin.getConfig().getBoolean("general.blockCommands.ifTopBidder", false);
    }

    @Override
    public boolean isBlockedCommand(String command) {
        if (!plugin.getConfig().isList("general.blockedCommands")) {
            return false;
        }
        return plugin.getConfig().getStringList("general.blockedCommands").contains(command);
    }

    @Override
    public boolean isBlockedMaterial(Material material) {
        if (!plugin.getConfig().isList("general.blockedMaterials")) {
            return false;
        }
        return plugin.getConfig().getStringList("general.blockedMaterials").contains(material.toString());
    }

    @Override
    public boolean isBlockedLore(String lore) {
        if (!plugin.getConfig().isList("general.blockedLore")) {
            return false;
        }
        List<String> blockedLore = plugin.getConfig().getStringList("general.blockedLore");
        for (String blocked : blockedLore) {
            if (blocked.contains(lore)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBroadcastTime(int time) {
        if (!plugin.getConfig().isList("general.broadcastTimes")) {
            return false;
        }
        return plugin.getConfig().getStringList("general.broadcastTimes").contains(Integer.toString(time));
    }
}

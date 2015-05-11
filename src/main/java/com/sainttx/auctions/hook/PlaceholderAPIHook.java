package com.sainttx.auctions.hook;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.api.AuctionsAPI;
import com.sainttx.auctions.util.TimeUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import org.bukkit.entity.Player;

/**
 * Fomratting for extended_clip's PlaceholderAPI
 */
public class PlaceholderAPIHook {

    /**
     * Registers all placeholders for the auction plugin
     */
    public static void registerPlaceHolders(final AuctionPlugin plugin) {
        PlaceholderAPI.registerPlaceholderHook(plugin, new PlaceholderHook() {
            @Override
            public String onPlaceholderRequest(Player player, String token) {
                Auction current = AuctionsAPI.getAuctionManager().getCurrentAuction();
                if (current == null) {
                    return "unknown";
                } else if (token.equalsIgnoreCase("itemamount")) {
                    return Integer.toString(current.getReward().getAmount());
                } else if (token.equalsIgnoreCase("time")) {
                    return TimeUtil.getFormattedTime(current.getTimeLeft());
                } else if (token.equalsIgnoreCase("autowin")) {
                    return plugin.formatDouble(current.getAutowin());
                } else if (token.equalsIgnoreCase("ownername")) {
                    return current.getOwnerName() == null ? "Console" : current.getOwnerName();
                } else if (token.equalsIgnoreCase("topbiddername")) {
                    return current.getTopBidderName() == null ? "Console" : current.getTopBidderName();
                } else if (token.equalsIgnoreCase("increment")) {
                    return plugin.formatDouble(current.getBidIncrement());
                } else if (token.equalsIgnoreCase("topbid")) {
                    return plugin.formatDouble(current.getTopBid());
                } else if (token.equalsIgnoreCase("taxpercent")) {
                    return plugin.formatDouble(current.getTax());
                } else if (token.equalsIgnoreCase("taxamount")) {
                    return plugin.formatDouble(current.getTaxAmount());
                } else if (token.equalsIgnoreCase("winnings")) {
                    return plugin.formatDouble(current.getTopBid() - current.getTaxAmount());
                } else if (token.equalsIgnoreCase("itemname")) {
                    return current.getReward().getName();
                } else if (token.equalsIgnoreCase("startprice")) {
                    return plugin.formatDouble(current.getStartPrice());
                } else {
                    return null;
                }
            }
        });
    }
}

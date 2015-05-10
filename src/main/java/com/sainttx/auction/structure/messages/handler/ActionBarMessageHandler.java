package com.sainttx.auction.structure.messages.handler;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.Auction;
import com.sainttx.auction.structure.messages.actionbar.ActionBarObject;
import com.sainttx.auction.structure.messages.actionbar.ActionBarObjectv1_8;
import com.sainttx.auction.structure.messages.actionbar.ActionBarObjectv1_8_3;
import com.sainttx.auction.util.ReflectionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 10/05/2015.
 */
public class ActionBarMessageHandler extends TextualMessageHandler {

    @Override
    public void broadcast(String message, Auction auction, boolean force) {
        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        message = formatter.format(plugin.getMessage("messages.auctionFormattable.actionBarMessage"), auction);

        if (!message.isEmpty()) {
            ActionBarObject actionBar = null;

            if (ReflectionUtil.getVersion().startsWith("v1_8_R2")) {
                actionBar = new ActionBarObjectv1_8_3(message);
            } else if (ReflectionUtil.getVersion().startsWith("v1_8_R1")) {
                actionBar = new ActionBarObjectv1_8(message);
            }

            if (actionBar != null) {
                for (CommandSender recipient : getAllRecipients()) {
                    if (recipient instanceof Player && !isIgnoring(((Player) recipient).getUniqueId())) {
                        actionBar.send((Player) recipient);
                    }
                }
            }
        }
    }
}

package com.sainttx.auctions.structure.messages.handler;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.api.Auction;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObject;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObjectv1_8_R1;
import com.sainttx.auctions.structure.messages.actionbar.ActionBarObjectv1_8_R3;
import com.sainttx.auctions.util.ReflectionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Matthew on 10/05/2015.
 */
public class ActionBarMessageHandler extends TextualMessageHandler {

    private ActionBarObject base;

    public ActionBarMessageHandler() {
        String version = ReflectionUtil.getVersion();
        if (version.startsWith("v1_8_R1")) {
            base = new ActionBarObjectv1_8_R1();
        } else if (version.startsWith("v1_8_R2")) {
            base = new ActionBarObjectv1_8_R3();
        } else {
            throw new IllegalStateException("this server version is unsupported");
        }
    }

    @Override
    public void broadcast(String message, Auction auction, boolean force) {
        super.broadcast(message, auction, force);

        AuctionPlugin plugin = AuctionPlugin.getPlugin();
        message = formatter.format(plugin.getMessage("messages.auctionFormattable.actionBarMessage"), auction);

        if (!message.isEmpty()) {
            base.setTitle(message);

            for (CommandSender recipient : getAllRecipients()) {
                if (recipient instanceof Player && !isIgnoring(recipient)) {
                    base.send((Player) recipient);
                }
            }
        }
    }
}

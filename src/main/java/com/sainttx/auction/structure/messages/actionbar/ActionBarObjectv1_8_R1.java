package com.sainttx.auction.structure.messages.actionbar;

import com.sainttx.auction.AuctionPlugin;
import com.sainttx.auction.api.AuctionsAPI;
import com.sainttx.auction.structure.messages.handler.TextualMessageHandler;
import com.sainttx.auction.util.ReflectionUtil;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by Matthew on 27/01/2015.
 */
public class ActionBarObjectv1_8_R1 extends ActionBarObject {

    private String rawTitle;

    public ActionBarObjectv1_8_R1() {
    }

    public ActionBarObjectv1_8_R1(String title) {
        this.setTitle(title);
    }

    @Override
    public void send(Player player) {
        try {
            Class<?> packetClazz = ReflectionUtil.getNMSClass("PacketPlayOutChat");
            Class<?> componentClazz = ReflectionUtil.getNMSClass("IChatBaseComponent");
            Class<?> serializerClazz = ReflectionUtil.getNMSClass("ChatSerializer");

            Constructor<?> packetContructor = ReflectionUtil.getConstructor(packetClazz, componentClazz, byte.class);

            Object baseComponentObj = ReflectionUtil.getMethod(serializerClazz, "a", String.class).invoke(null, convert(rawTitle));
            Object packet = packetContructor.newInstance(baseComponentObj, (byte) 2);

            Object playerConnection = ReflectionUtil.getConnection(player);
            Method method = ReflectionUtil.getMethod(playerConnection.getClass(), "sendPacket", ReflectionUtil.getNMSClass("Packet"));
            method.invoke(playerConnection, packet);
        } catch (Throwable t) {
            AuctionPlugin.getPlugin().getLogger().severe("Failed to send action bar");
            AuctionsAPI.getAuctionManager().setMessageHandler(new TextualMessageHandler());
            AuctionPlugin.getPlugin().getLogger().info("Message handler has been set to TEXT as a safety precaution");
        }
    }

    @Override
    public void setTitle(String title) {
        this.rawTitle = title;
    }
}

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

package com.sainttx.auctions.structure.messages.actionbar;

import com.sainttx.auctions.AuctionPlugin;
import com.sainttx.auctions.structure.messages.handler.TextualMessageHandler;
import com.sainttx.auctions.util.ReflectionUtil;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by Matthew on 27/01/2015.
 */
public class ActionBarObjectv1_9_R1 extends ActionBarObject {

    private AuctionPlugin plugin;
    private String rawTitle;

    public ActionBarObjectv1_9_R1(AuctionPlugin plugin) {
        this(plugin, null);
    }

    public ActionBarObjectv1_9_R1(AuctionPlugin plugin, String title) {
        this.plugin = plugin;
        this.setTitle(title);
    }

    @Override
    public void send(Player player) {
        Class<?> packetClazz = ReflectionUtil.getNMSClass("PacketPlayOutChat");
        Class<?> componentClazz = ReflectionUtil.getNMSClass("IChatBaseComponent");
        Class<?> chatComponentClazz = ReflectionUtil.getNMSClass("ChatComponentText");
        Constructor<?> packetContructor = ReflectionUtil.getConstructor(packetClazz, componentClazz, byte.class);

        try {
            Constructor<?> chatComponentConstructor = ReflectionUtil.getConstructor(chatComponentClazz, String.class);
            Object componentObj = chatComponentConstructor.newInstance(rawTitle);
            Object packet = packetContructor.newInstance(componentObj, (byte) 2);
            Object playerConnection = ReflectionUtil.getConnection(player);
            Method method = ReflectionUtil.getMethod(playerConnection.getClass(), "sendPacket", ReflectionUtil.getNMSClass("Packet"));
            method.invoke(playerConnection, packet);
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to send action bar");
            plugin.getManager().setMessageHandler(new TextualMessageHandler(plugin));
            plugin.getLogger().info("Message handler has been set to TEXT as a safety precaution");
            t.printStackTrace();
        }
    }

    @Override
    public void setTitle(String title) {
        this.rawTitle = title;
    }
}
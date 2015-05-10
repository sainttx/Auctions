package com.sainttx.auction.structure.messages.actionbar;

import com.sainttx.auction.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by Matthew on 27/01/2015.
 */
public class ActionBarObjectv1_8_3 implements ActionBarObject  {
    private String rawTitle;

    public ActionBarObjectv1_8_3(String title) {
        this.setTitle(title);
    }

    public void broadcast() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player);
        }
    }

    public void send(Player player) {
        Class<?> packetClazz = ReflectionUtil.getNMSClass("PacketPlayOutChat");
        Class<?> componentClazz = ReflectionUtil.getNMSClass("IChatBaseComponent");
        Class<?> serializerClazz = ReflectionUtil.getNMSClass("IChatBaseComponent$ChatSerializer");

        Constructor<?> packetContructor = ReflectionUtil.getConstructor(packetClazz, componentClazz, byte.class);

        try {
            Object baseComponentObj = ReflectionUtil.getMethod(serializerClazz, "a", String.class).invoke(null, convert(rawTitle));
            Object packet = packetContructor.newInstance(baseComponentObj, (byte) 2);

            Object playerConnection = ReflectionUtil.getConnection(player);
            Method method = ReflectionUtil.getMethod(playerConnection.getClass(), "sendPacket", ReflectionUtil.getNMSClass("Packet"));
            method.invoke(playerConnection, packet);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public String getTitle() {
        return this.rawTitle;
    }

    public void setTitle(String title) {
        this.rawTitle = title;
        // this.formattedTitle = this.title.a(convert(title));
    }

    /**
     * Converts a raw string into a valid string format for a title
     *
     * @param text A raw text string
     * @return A converted string that can be used for titles
     */
    public static String convert(String text) {
        if (text == null || text.length() == 0) {
            return "\"\"";
        }

        char c;
        int i;
        int len = text.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len ; i += 1) {
            c = text.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u" + t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }

        sb.append('"');
        return sb.toString();
    }
}

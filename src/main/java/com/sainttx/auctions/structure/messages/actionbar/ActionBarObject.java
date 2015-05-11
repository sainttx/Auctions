package com.sainttx.auctions.structure.messages.actionbar;

import org.bukkit.entity.Player;

/**
 * Created by Matthew on 10/05/2015.
 */
public abstract class ActionBarObject {

    /**
     * Sends the action bar to a player
     *
     * @param player the player to send the action bar too
     */
    public abstract void send(Player player);

    /**
     * Sets the action bars title
     *
     * @param title the new title
     */
    public abstract void setTitle(String title);

    /**
     * Converts a raw string into a valid string format for a title
     *
     * @param text A raw text string
     * @return A converted string that can be used for titles
     */
    public String convert(String text) {
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

package com.sainttx.auctions.api.messages;

public interface MessageGroupParser {

    /**
     * Parses a message group from a string of text.
     *
     * @param text the text
     * @return the group
     */
    MessageGroup parse(String text);

    /**
     * Returns <tt>true</tt> if a string can be parsed by this parser.
     *
     * @param text the text
     * @return <tt>true</tt> if condition is met
     */
    boolean isValid(String text);
}

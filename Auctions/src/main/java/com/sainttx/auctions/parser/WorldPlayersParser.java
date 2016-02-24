package com.sainttx.auctions.parser;

import com.sainttx.auctions.api.messages.MessageGroup;
import com.sainttx.auctions.api.messages.MessageGroupParser;
import com.sainttx.auctions.api.messages.WorldPlayersGroup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldPlayersParser implements MessageGroupParser {

    private Pattern pattern = Pattern.compile("world:(.+)", Pattern.CASE_INSENSITIVE);

    @Override
    public MessageGroup parse(String text) {
        Matcher matcher = pattern.matcher(text);
        String world = matcher.group(1);
        return new WorldPlayersGroup(world);
    }

    @Override
    public boolean isValid(String text) {
        return pattern.matcher(text).matches();
    }
}

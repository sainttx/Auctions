package com.sainttx.auctions.parser;

import com.sainttx.auctions.api.messages.MessageGroup;
import com.sainttx.auctions.api.messages.MessageGroupParser;
import com.sainttx.auctions.recipient.HerochatChannelGroup;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HerochatGroupParser implements MessageGroupParser {

    private Pattern pattern = Pattern.compile("herochat:(.+)", Pattern.CASE_INSENSITIVE);

    @Override
    public MessageGroup parse(String text) {
        Matcher matcher = pattern.matcher(text);
        String channel = matcher.group(1);
        return new HerochatChannelGroup(channel);
    }

    @Override
    public boolean isValid(String text) {
        return Bukkit.getPluginManager().isPluginEnabled("Herochat")
                && pattern.matcher(text).matches();
    }
}

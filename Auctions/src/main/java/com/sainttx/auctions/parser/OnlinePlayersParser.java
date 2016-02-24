package com.sainttx.auctions.parser;

import com.sainttx.auctions.api.messages.MessageGroup;
import com.sainttx.auctions.api.messages.MessageGroupParser;
import com.sainttx.auctions.api.messages.OnlinePlayersGroup;

public class OnlinePlayersParser implements MessageGroupParser {

    @Override
    public MessageGroup parse(String text) {
        return new OnlinePlayersGroup();
    }

    @Override
    public boolean isValid(String text) {
        return text.equalsIgnoreCase("global");
    }
}

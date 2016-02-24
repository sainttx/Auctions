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

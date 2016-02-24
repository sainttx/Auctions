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

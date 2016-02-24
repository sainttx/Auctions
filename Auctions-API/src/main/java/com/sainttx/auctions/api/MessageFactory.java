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

package com.sainttx.auctions.api;

import com.sainttx.auctions.api.messages.MessageGroup;
import org.bukkit.command.CommandSender;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public interface MessageFactory {

    /**
     * Adds a message group to receive messages submitted through
     * {@link #submitBroadcast(Message)} or {@link #submitBroadcast(Message, Auction)}.
     *
     * @param group the group
     * @return <tt>true</tt> if the groups that are receiving messages changed
     */
    boolean addMessageGroup(MessageGroup group);

    /**
     * Removes a message group from receiving broadcasts.
     *
     * @param group the group
     * @return <tt>true</tt> if the group was removed as a result of this call
     */
    boolean removeMessageGroup(MessageGroup group);

    /**
     * Submits a task to fetch, format, and send a message to a recipient
     * and returns a Future representing that task. The Future's {@code get}
     * method will return {@code null} upon <em>successful</em> completion.
     *
     * @param recipient the sender that is receiving the message.
     * @param message the message object to send.
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submit(final CommandSender recipient, final Message message);

    /**
     * Submits a task to fetch, format, and send a message to a recipient and
     * returns a Future representing that task. The task will format any auction
     * specific placeholders that the message contains using the variables of
     * the provided {@link Auction}. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param recipient the sender that is receiving the message.
     * @param message the message object to send.
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submit(final CommandSender recipient, final Message message, final Auction auction);

    /**
     * Submits a task to fetch, format, and send a message to all registered
     * message groups and returns a Future representing that task. The Future's
     * {@code get} method will return {@code null} upon <em>successful</em>
     * completion.
     *
     * @param message the message object to send.
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submitBroadcast(final Message message);

    /**
     * Submits a task to fetch, format, and send a message to all registered message
     * groups and returns a Future representing that task. The task will format any
     * auction specific placeholders that the message contains using the variables
     * of the provided {@link Auction}. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param message the message object to send.
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<?> submitBroadcast(final Message message, final Auction auction);
}

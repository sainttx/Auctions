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

package com.sainttx.auctions.util;

import com.sainttx.auctions.AuctionPlugin;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * A utility class that formats {@link Long} based time diffs into english
 */
public class TimeUtil {

    static int dateDiff(int type, Calendar fromDate, Calendar toDate, boolean future) {
        int diff = 0;
        long savedDate = fromDate.getTimeInMillis();
        while ((future && !fromDate.after(toDate)) || (!future && !fromDate.before(toDate))) {
            savedDate = fromDate.getTimeInMillis();
            fromDate.add(type, future ? 1 : -1);
            diff++;
        }
        diff--;
        fromDate.setTimeInMillis(savedDate);
        return diff;
    }

    public static String formatDateDiff(long date, boolean shortened) {
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(date);
        Calendar now = new GregorianCalendar();
        return formatDateDiff(now, c, shortened);
    }

    public static String formatDateDiff(Calendar fromDate, Calendar toDate, boolean shortened) {
        boolean future = false;
        if (toDate.equals(fromDate)) {
            return "0 seconds";
        }
        if (toDate.after(fromDate)) {
            future = true;
        }
        StringBuilder sb = new StringBuilder();
        int[] types = new int[]{Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};
        String[] names = new String[]{ "year", "years", "month", "months", "day", "days", "hour", "hours", "minute", "minutes", "second", "seconds" };
        int accuracy = 0;
        for (int i = 0; i < types.length; i++) {
            if (accuracy > 2) {
                break;
            }
            int diff = dateDiff(types[i], fromDate, toDate, future);

            if (diff > 0) {
                accuracy++;

                if (shortened) {
                    sb.append(diff);
                } else {
                    sb.append(" ").append(diff).append(" ");
                }

                String name = names[i * 2 + (diff > 1 ? 1 : 0)];
                if (shortened) {
                    name = Character.toString(name.toCharArray()[0]);
                }

                sb.append(name + (shortened ? ", " : ""));
            }
        }
        if (sb.length() == 0) {
            return "now";
        }
        if (shortened) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString().trim();
    }

    /**
     * Return a String representation of time left
     *
     * @param timeLeft Time left in seconds
     * @return String the time left
     */
    public static String getFormattedTime(int timeLeft) {
        return formatDateDiff(System.currentTimeMillis() + (timeLeft * 1000L),
                AuctionPlugin.getPlugin().getConfig().getBoolean("general.shortenedTimeFormat", false));
    }
}

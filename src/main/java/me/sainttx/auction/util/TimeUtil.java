package me.sainttx.auction.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Matthew on 30/01/2015.
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

    public static String formatDateDiff(long date) {
        return formatDateDiff(date, false);
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
            return "&6Now";
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
}

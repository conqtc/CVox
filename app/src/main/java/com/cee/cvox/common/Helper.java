package com.cee.cvox.common;

import android.graphics.Color;
import android.text.Html;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by conqtc on 10/18/17.
 */

public class Helper {
    public static Calendar calendar = Calendar.getInstance();

    /**
     * @param date
     * @param days
     * @return
     */
    public static Date plusDays(Date date, int days) {
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    /**
     * @param date1
     * @param date2
     * @return
     */
    public static long daysBetweenDates(Date date1, Date date2) {
        long milliseconds = Math.abs(date1.getTime() - date2.getTime());
        return TimeUnit.DAYS.convert(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * @param date
     * @param format
     * @return
     */
    public static String dateToString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static Date stringToDate(String text, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            Date date = (Date) sdf.parse(text);
            return date;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * @param milli
     * @return
     */
    public static Date milliToDate(long milli) {
        calendar.setTimeInMillis(milli);
        return calendar.getTime();
    }

    /**
     * @param milli
     * @param pattern
     * @return
     */
    public static String milliToString(long milli, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(milliToDate(milli));
    }

    /**
     * @param link
     * @return
     */
    public static String extractGuidFromLink(String link) {
        String[] segments = link.split("/");
        return segments[segments.length - 1];
    }

    /**
     * @param html
     * @return
     */
    public static String fromHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } else {
            return Html.fromHtml(html).toString().trim();
        }
    }

    /**
     * @param bytes
     * @return
     */
    public static String sizeFromBytes(long bytes) {
        double size = bytes;

        if (bytes < 1024) {
            return String.format("%d Bytes", bytes);
        }

        double kb = size / 1024;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }

        double mb = size / (1024 * 1024);
        return String.format("%.1f MB", mb);
    }

    /**
     * @param seconds
     * @return
     */
    public static String timeFromSeconds(double seconds) {
        long s = (long) seconds;

        long m = s / 60;
        s = s % 60;

        long h = m / 60;
        m = m % 60;

        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        } else {
            return String.format("%02d:%02d s", m, s);
        }
    }

    /**
     * @param color
     * @return
     */
    public static int getColorBrightness(int color) {
        if (color == Color.TRANSPARENT) return 255;

        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        // return 0 - 255
        return (int) Math.sqrt(rgb[0] * rgb[0] * .241 +
                rgb[1] * rgb[1] * .691 +
                rgb[2] * rgb[2] * .068);

    }
}

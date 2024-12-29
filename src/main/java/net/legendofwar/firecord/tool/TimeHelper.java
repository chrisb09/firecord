package net.legendofwar.firecord.tool;

import java.util.concurrent.TimeUnit;

public class TimeHelper {

    public static String getHumanReadableDuration(long duration, TimeUnit unit) {

        //TODO: move to minilib and use predefined texts

        long rawNanoTime = unit.toNanos(duration);

        if (rawNanoTime < 0) {
            return "negative time";
        }

        // Define the conversions for each time unit in nanoseconds
        long ns = 1;
        long µs = ns * 1000;
        long ms = µs * 1000;
        long second = ms * 1000;
        long minute = second * 60;
        long hour = minute * 60;
        long day = hour * 24;
        long week = day * 7;
        long month = day * 30;
        long year = day * 365;

        // Calculate each unit amount
        long years = rawNanoTime / year;
        rawNanoTime %= year;
        long months = rawNanoTime / month;
        rawNanoTime %= month;
        long weeks = rawNanoTime / week;
        rawNanoTime %= week;
        long days = rawNanoTime / day;
        rawNanoTime %= day;
        long hours = rawNanoTime / hour;
        rawNanoTime %= hour;
        long minutes = rawNanoTime / minute;
        rawNanoTime %= minute;
        long seconds = rawNanoTime / second;
        rawNanoTime %= second;
        long milliseconds = rawNanoTime / ms;
        rawNanoTime %= ms;
        long microseconds = rawNanoTime / µs;
        rawNanoTime %= µs;
        long nanoseconds = rawNanoTime;

        // Create an array to hold the calculated values and their labels
        long[] values = { years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds };
        String[] labels = { "year", "month", "week", "day", "hour", "minute", "second", "ms", "µs", "ns" };

        // Build the human-readable string
        StringBuilder result = new StringBuilder();
        int nonZeroCount = 0;

        for (int i = 0; i < values.length && nonZeroCount < 2; i++) {
            if (values[i] > 0) {
                if (nonZeroCount > 0) {
                    result.append(" ");
                }
                result.append(values[i]).append(" ").append(labels[i]);
                if (values[i] > 1) {
                    if (i < 7){
                        result.append("");  // Pluralize
                    }
                }
                nonZeroCount++;
            }
        }

        return result.toString().isEmpty() ? "0s" : result.toString();
    }
    
}

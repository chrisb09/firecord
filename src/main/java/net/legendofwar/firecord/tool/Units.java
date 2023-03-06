package net.legendofwar.firecord.tool;

import java.util.ArrayList;

public class Units {

    public static String getTimeDelta(long ns) {
        final String[] units = { "y", "mon", "d", "h", "min", "s", "ms", "µs", "ns" };
        final long[] unitlen = { 12l * 30l * 24l * 60l * 60l * 1000l * 1000l * 1000l, 30l * 24l * 60l * 60l * 1000l * 1000l * 1000l,
            24l * 60l * 60l * 1000l * 1000l * 1000l, 60l * 60l * 1000l * 1000l * 1000l, 60l * 1000l * 1000l * 1000l,
            1000l * 1000l * 1000l,
            1000l * 1000l, 1000l, 1l };
        int biggest_fit = -1;
        for (int i = 0; i < units.length; i++) {
            if (ns >= unitlen[i]) {
                biggest_fit = i;
                break;
            }
        }
        ArrayList<String> list = new ArrayList<>();
        for (int i = biggest_fit; i < Math.min(biggest_fit + 3, unitlen.length); i++) {
            list.add(ns / unitlen[i] + units[i]);
            ns = ns % unitlen[i];
        }
        return String.join(" ", list);
    }

}

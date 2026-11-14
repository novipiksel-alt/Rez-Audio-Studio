// SPDX-License-Identifier: Apache-2.0
package rs.rez.audio;

import java.util.Locale;

/** Shared range rules: timestamps are microseconds throughout the audio engine. */
public final class TimeRange {
    public final long startUs, endUs;
    public TimeRange(long startUs, long endUs) {
        if (startUs < 0 || endUs <= startUs) throw new IllegalArgumentException("Kraj mora biti poslije početka.");
        this.startUs = startUs; this.endUs = endUs;
    }
    public static long parse(String text) {
        String[] parts = text.trim().replace(',', '.').split(":", -1);
        if (parts.length > 3 || parts.length == 0) throw new IllegalArgumentException("Unesite mm:ss.SSS ili sekunde.");
        double seconds = 0;
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches(i == parts.length - 1 ? "[0-9]+(\\.[0-9]{1,6})?" : "[0-9]+"))
                throw new IllegalArgumentException("Unesite mm:ss.SSS ili sekunde.");
            double value = Double.parseDouble(parts[i]);
            if (parts.length > 1 && i > 0 && value >= 60) throw new IllegalArgumentException("Sekunde i minuti moraju biti manji od 60.");
            seconds = seconds * 60 + value;
        }
        if (!Double.isFinite(seconds) || seconds > Long.MAX_VALUE / 1_000_000d) throw new IllegalArgumentException("Vrijeme je preveliko.");
        return Math.round(seconds * 1_000_000d);
    }
    public static String format(long us) {
        long ms = Math.max(0, us) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d.%03d", ms / 60000, ms / 1000 % 60, ms % 1000);
    }
    public static String safeName(String input, String extension) {
        String result = input.trim().replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", "_");
        if (result.toLowerCase(Locale.ROOT).endsWith("." + extension)) result = result.substring(0, result.length() - extension.length() - 1);
        if (result.isEmpty() || result.equals(".") || result.equals("..")) throw new IllegalArgumentException("Upišite naziv isječka.");
        if (result.length() > 100) result = result.substring(0, 100);
        return result + "." + extension;
    }
}

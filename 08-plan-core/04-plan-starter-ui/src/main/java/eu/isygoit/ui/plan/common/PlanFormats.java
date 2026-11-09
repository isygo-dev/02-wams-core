package eu.isygoit.ui.plan.common;

import eu.isygoit.ui.common.component.BidiLtr;
import eu.isygoit.ui.common.component.LocaleAwareDateFormat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Display formats of the plan screens: instants follow the application date convention
 * ({@link LocaleAwareDateFormat}) in the server time zone, durations are shown as {@code HH:mm:ss}.
 */
public final class PlanFormats {

    private static final String NONE = "-";

    private PlanFormats() {
    }

    /**
     * Date and time ({@code dd/MM/yyyy HH:mm}); a dash when missing.
     */
    public static String dateTime(Instant instant) {
        if (instant == null) {
            return NONE;
        }
        return BidiLtr.wrap(LocaleAwareDateFormat.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault())));
    }

    /**
     * Date and time with seconds; a dash when missing.
     */
    public static String dateTimeSeconds(Instant instant) {
        if (instant == null) {
            return NONE;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(LocaleAwareDateFormat.dateTimePattern() + ":ss");
        return BidiLtr.wrap(formatter.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault())));
    }

    /**
     * Elapsed time between two instants; a dash when one of them is missing.
     */
    public static String duration(Instant start, Instant end) {
        if (start == null || end == null || end.isBefore(start)) {
            return NONE;
        }
        return millis(Duration.between(start, end).toMillis());
    }

    /**
     * A duration in milliseconds: {@code N ms} below one second, otherwise {@code HH:mm:ss}.
     */
    public static String millis(long millis) {
        if (millis < 0) {
            return NONE;
        }
        if (millis < 1000) {
            return millis + " ms";
        }
        long seconds = millis / 1000;
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}

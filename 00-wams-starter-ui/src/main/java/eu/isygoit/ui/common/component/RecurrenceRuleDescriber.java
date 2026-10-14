package eu.isygoit.ui.common.component;

import eu.isygoit.i18n.I18n;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an iCalendar RRULE string (RFC 5545) into a human-readable sentence,
 * e.g. {@code FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE;UNTIL=20260630T235959}
 * becomes <em>"Every 2 weeks on Monday and Wednesday, until 30/06/2026 at 23:59"</em>.
 *
 * <p>Only the subset produced by {@code RecurrenceRuleBuilderDialog} is fully
 * translated. Unknown keys are ignored — the raw string stays available in
 * the field, so nothing is lost.
 */
public final class RecurrenceRuleDescriber {

    private static final DateTimeFormatter UNTIL_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private RecurrenceRuleDescriber() {
    }

    /** Full sentence describing the rule, or an explanatory text when empty/invalid. */
    public static String describe(String rule) {
        if (rule == null || rule.isBlank()) {
            return I18n.t("calendar.recurrence.summary.none");
        }
        Map<String, String> parts = parse(rule);
        if (!parts.containsKey("FREQ")) {
            return I18n.t("calendar.recurrence.summary.invalid");
        }

        StringBuilder sb = new StringBuilder();
        String freq = parts.get("FREQ").toUpperCase();
        int interval = parsePositiveInt(parts.get("INTERVAL"), 1);
        sb.append(freqPhrase(freq, interval)).append(" ");

        if (parts.containsKey("BYDAY")) {
            String days = joinDays(parts.get("BYDAY"));
            if (!days.isBlank()) {
                sb.append(I18n.t("calendar.recurrence.summary.on.days", days)).append(" ");
            }
        }
        if (parts.containsKey("BYMONTHDAY")) {
            sb.append(I18n.t("calendar.recurrence.summary.on.dayOfMonth",
                    parts.get("BYMONTHDAY"))).append(" ");
        }

        if (parts.containsKey("COUNT")) {
            sb.append(I18n.t("calendar.recurrence.summary.count", parts.get("COUNT"))).append(" ");
        } else if (parts.containsKey("UNTIL")) {
            sb.append(I18n.t("calendar.recurrence.summary.until",
                    formatUntil(parts.get("UNTIL")))).append(" ");
        } else {
            sb.append(I18n.t("calendar.recurrence.summary.neverEnds")).append(" ");
        }

        return sb.toString();
    }

    /**
     * Extracts the {@code UNTIL} value from the rule as a {@link LocalDateTime},
     * or {@code null} when the rule has no {@code UNTIL} part. Used to keep the
     * {@code CalendarEventDto.recurrenceEnd} field in sync without a dedicated
     * picker.
     */
    public static LocalDateTime extractUntil(String rule) {
        if (rule == null || rule.isBlank()) return null;
        Map<String, String> parts = parse(rule);
        String until = parts.get("UNTIL");
        if (until == null || until.isBlank()) return null;
        try {
            return LocalDateTime.parse(until, UNTIL_FMT);
        } catch (Exception ex) {
            return null;
        }
    }

    /* ══════════════════════════════════════════════════════════════
     * Internals
     * ══════════════════════════════════════════════════════════════ */

    private static Map<String, String> parse(String rule) {
        Map<String, String> parts = new LinkedHashMap<>();
        for (String part : rule.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            parts.put(part.substring(0, eq).trim().toUpperCase(),
                    part.substring(eq + 1).trim());
        }
        return parts;
    }

    private static String freqPhrase(String freq, int interval) {
        if (interval <= 1) {
            return I18n.t("calendar.recurrence.summary.freq." + freq);
        }
        return I18n.t("calendar.recurrence.summary.freq." + freq + ".interval",
                String.valueOf(interval));
    }

    private static String joinDays(String byday) {
        List<String> labels = new ArrayList<>();
        for (String code : byday.split(",")) {
            String c = code.trim().toUpperCase();
            if (!c.isEmpty()) labels.add(I18n.t("calendar.recurrence.day." + c));
        }
        if (labels.isEmpty()) return "";
        if (labels.size() == 1) return labels.get(0);
        String last = labels.remove(labels.size() - 1);
        return String.join(", ", labels) + " " + I18n.t("common.and") + " " + last;
    }

    private static String formatUntil(String until) {
        try {
            LocalDateTime dt = LocalDateTime.parse(until, UNTIL_FMT);
            return I18n.t("calendar.recurrence.summary.until.format",
                    dt.toLocalDate().format(DATE_FMT),
                    dt.toLocalTime().format(TIME_FMT));
        } catch (Exception ex) {
            return until;
        }
    }

    private static int parsePositiveInt(String s, int fallback) {
        if (s == null) return fallback;
        try {
            int v = Integer.parseInt(s);
            return v > 0 ? v : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
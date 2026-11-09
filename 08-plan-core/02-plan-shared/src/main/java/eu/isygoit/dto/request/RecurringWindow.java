package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * A recurring daily window in CALENDAR-LOCAL time (the calendar timezone), expanded by the calendar expander over
 * the horizon (DST aware). Used for recurring working windows and for recurring breaks.
 * <pre>{@code
 * { "days": ["MONDAY","TUESDAY"], "from": "08:00", "to": "12:00", "validFrom": "2026-09-01", "validTo": null }
 * }</pre>
 *
 * @param days      days of the week the window applies to; empty = every day
 * @param from      local start time
 * @param to        local end time; {@code to <= from} means "crosses midnight" ({@code to == from} = 24 h)
 * @param validFrom first date (inclusive) the rule applies, null = unbounded
 * @param validTo   last date (inclusive) the rule applies, null = unbounded
 */
public record RecurringWindow(
        Set<DayOfWeek> days,
        LocalTime from,
        LocalTime to,
        LocalDate validFrom,
        LocalDate validTo) {

    public RecurringWindow {
        days = CollectionCopies.enumSet(days);
    }
}

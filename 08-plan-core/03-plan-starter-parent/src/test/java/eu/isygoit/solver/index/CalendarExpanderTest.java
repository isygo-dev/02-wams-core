package eu.isygoit.solver.index;

import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.RecurringWindow;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.exception.LimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendarExpanderTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static Horizon horizon(String start, String end, int tickSeconds) {
        return new Horizon(Instant.parse(start), Instant.parse(end), tickSeconds);
    }

    private static Calendar calendar(String timezone, List<TimeWindow> windows, List<RecurringWindow> recurring,
                                     List<TimeWindow> exceptions, List<RecurringWindow> breaks) {
        return new Calendar("cal", "cal", timezone, windows, recurring, exceptions, breaks);
    }

    private static RecurringWindow daily(String from, String to, DayOfWeek... days) {
        return new RecurringWindow(days.length == 0 ? null : Set.of(days), LocalTime.parse(from), LocalTime.parse(to),
                null, null);
    }

    private static TimeWindow window(String from, String to) {
        return new TimeWindow(Instant.parse(from), Instant.parse(to));
    }

    // ---------------------------------------------------------------------------------------- explicit windows

    @Test
    void explicitWindowsAreMergedAndExceptionsSubtracted() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        Calendar cal = calendar(null,
                List.of(window("2026-01-05T08:00:00Z", "2026-01-05T12:00:00Z"),
                        window("2026-01-05T10:00:00Z", "2026-01-05T14:00:00Z")),
                null, List.of(window("2026-01-05T11:00:00Z", "2026-01-05T12:00:00Z")), null);
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(8, 11, 12, 14);
    }

    @Test
    void calendarWithoutWindowsIsAlwaysAvailableMinusExceptionsAndBreaks() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        assertThat(CalendarExpander.expand(calendar(null, null, null, null, null), h, UTC, null))
                .containsExactly(0, 24);
        Calendar withHoliday = calendar(null, null, null,
                List.of(window("2026-01-05T06:00:00Z", "2026-01-05T08:00:00Z")), null);
        assertThat(CalendarExpander.expand(withHoliday, h, UTC, null)).containsExactly(0, 6, 8, 24);
    }

    @Test
    void windowsOutsideTheHorizonAreClippedWithAWarning() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        Calendar cal = calendar(null, List.of(window("2026-01-04T20:00:00Z", "2026-01-05T04:00:00Z")), null, null, null);
        List<DiagnosticIssue> warnings = new ArrayList<>();
        assertThat(CalendarExpander.expand(cal, h, UTC, warnings)).containsExactly(0, 4);
        assertThat(warnings).extracting(DiagnosticIssue::code).contains("OUT_OF_HORIZON_CLIPPED");
    }

    // ---------------------------------------------------------------------------------------- rounding

    @Test
    void availabilityShrinksAndUnavailabilityGrowsWhenRounding() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        Calendar cal = calendar(null,
                List.of(window("2026-01-05T08:30:00Z", "2026-01-05T12:30:00Z")), null,
                List.of(window("2026-01-05T10:15:00Z", "2026-01-05T10:45:00Z")), null);
        List<DiagnosticIssue> warnings = new ArrayList<>();
        // availability [9,12) (start up, end down), blocked [10,11) (start down, end up)
        assertThat(CalendarExpander.expand(cal, h, UTC, warnings)).containsExactly(9, 10, 11, 12);
        assertThat(warnings).extracting(DiagnosticIssue::code).contains("CALENDAR_ROUNDED");
    }

    // ---------------------------------------------------------------------------------------- recurring

    @Test
    void recurringWindowsExpandOnTheSelectedDays() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-07T00:00:00Z", 3600); // Mon + Tue
        Calendar cal = calendar(null, null,
                List.of(daily("09:00", "17:00", DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.SUNDAY)), null, null);
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(9, 17, 33, 41);
        Calendar mondayOnly = calendar(null, null, List.of(daily("09:00", "17:00", DayOfWeek.MONDAY)), null, null);
        assertThat(CalendarExpander.expand(mondayOnly, h, UTC, null)).containsExactly(9, 17);
    }

    @Test
    void recurringBreaksAreSubtracted() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-07T00:00:00Z", 3600);
        Calendar cal = calendar(null, null, List.of(daily("09:00", "17:00")), null,
                List.of(daily("12:00", "13:00")));
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(9, 12, 13, 17, 33, 36, 37, 41);
    }

    @Test
    void overnightWindowsCrossMidnightAndTheHorizonEdges() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-07T00:00:00Z", 3600);
        Calendar cal = calendar(null, null, List.of(daily("22:00", "06:00")), null, null);
        // the window that started the evening before the horizon is clipped to [0,6); the last one to [46,48)
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(0, 6, 22, 30, 46, 48);
    }

    @Test
    void equalFromAndToMeansTwentyFourHours() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-07T00:00:00Z", 3600);
        Calendar cal = calendar(null, null, List.of(daily("00:00", "00:00")), null, null);
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(0, 48);
    }

    @Test
    void validityRangeBoundsTheStartDate() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-07T00:00:00Z", 3600);
        RecurringWindow fromTuesday = new RecurringWindow(null, LocalTime.parse("09:00"), LocalTime.parse("10:00"),
                LocalDate.parse("2026-01-06"), null);
        assertThat(CalendarExpander.expand(calendar(null, null, List.of(fromTuesday), null, null), h, UTC, null))
                .containsExactly(33, 34);
        RecurringWindow untilMonday = new RecurringWindow(null, LocalTime.parse("09:00"), LocalTime.parse("10:00"),
                null, LocalDate.parse("2026-01-05"));
        assertThat(CalendarExpander.expand(calendar(null, null, List.of(untilMonday), null, null), h, UTC, null))
                .containsExactly(9, 10);
    }

    // ---------------------------------------------------------------------------------------- DST

    @Test
    void workingDaysFollowTheLocalClockAcrossTheSpringForwardChange() {
        // Europe/Paris: Sat 28 March CET (+1), Sun 29 and Mon 30 March CEST (+2)
        Horizon h = horizon("2026-03-28T00:00:00Z", "2026-03-31T00:00:00Z", 3600);
        Calendar cal = calendar("Europe/Paris", null, List.of(daily("08:00", "17:00")), null, null);
        // Sat 07:00Z-16:00Z, Sun 06:00Z-15:00Z, Mon 06:00Z-15:00Z
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(7, 16, 30, 39, 54, 63);
    }

    @Test
    void aNonexistentLocalStartMovesForwardOverTheDstGap() {
        Horizon h = horizon("2026-03-28T00:00:00Z", "2026-03-31T00:00:00Z", 3600);
        // Sunday 29 March 2026: 02:00 does not exist in Paris -> 03:00 CEST (01:00Z); 04:00 CEST = 02:00Z
        Calendar cal = calendar("Europe/Paris", null, List.of(daily("02:00", "04:00", DayOfWeek.SUNDAY)), null, null);
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(25, 26);
    }

    @Test
    void anAmbiguousLocalStartTakesTheEarlierOffsetOverTheDstOverlap() {
        Horizon h = horizon("2026-10-25T00:00:00Z", "2026-10-26T00:00:00Z", 1800);
        // Sunday 25 October 2026: 02:30 occurs twice in Paris; the earlier is CEST (+2) = 00:30Z; end 04:00 CET = 03:00Z
        Calendar cal = calendar("Europe/Paris", null, List.of(daily("02:30", "04:00", DayOfWeek.SUNDAY)), null, null);
        assertThat(CalendarExpander.expand(cal, h, UTC, null)).containsExactly(1, 6);
    }

    @Test
    void theCalendarZoneOverridesTheDefaultZone() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        Calendar withoutZone = calendar(null, null, List.of(daily("09:00", "10:00")), null, null);
        Calendar tokyo = calendar("Asia/Tokyo", null, List.of(daily("09:00", "10:00")), null, null);
        assertThat(CalendarExpander.expand(withoutZone, h, ZoneId.of("Asia/Tokyo"), null)).containsExactly(0, 1);
        assertThat(CalendarExpander.expand(tokyo, h, UTC, null)).containsExactly(0, 1);
        assertThat(CalendarExpander.expand(withoutZone, h, UTC, null)).containsExactly(9, 10);
    }

    @Test
    void anInvalidZoneIsRejected() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        Calendar cal = calendar("Mars/Olympus", null, List.of(daily("09:00", "10:00")), null, null);
        assertThatThrownBy(() -> CalendarExpander.expand(cal, h, UTC, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------------------------------- budget and helpers

    @Test
    void theRunBudgetIsEnforced() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-15T00:00:00Z", 3600);
        Calendar cal = calendar(null, null, List.of(daily("09:00", "10:00")), null, null);
        assertThatThrownBy(() -> CalendarExpander.expand(cal, h, UTC, null, 3))
                .isInstanceOf(LimitExceededException.class)
                .hasMessageContaining("max-calendar-runs");
    }

    @Test
    void normalizeSortsAndMergesTouchingRuns() {
        assertThat(CalendarExpander.normalize(new int[]{5, 8, 1, 3, 3, 4})).containsExactly(1, 4, 5, 8);
        assertThat(CalendarExpander.normalize(new int[0])).isEmpty();
    }

    @Test
    void subtractAndIntersectWorkOnRuns() {
        assertThat(CalendarExpander.subtract(new int[]{0, 10}, new int[]{2, 4, 6, 8}))
                .containsExactly(0, 2, 4, 6, 8, 10);
        assertThat(CalendarExpander.subtract(new int[]{0, 10}, new int[]{0, 10})).isEmpty();
        assertThat(CalendarExpander.subtract(new int[]{0, 10}, new int[0])).containsExactly(0, 10);
        assertThat(CalendarExpander.intersect(new int[]{0, 5, 7, 10}, new int[]{3, 8})).containsExactly(3, 5, 7, 8);
        assertThat(CalendarExpander.intersect(new int[]{0, 5}, new int[]{5, 8})).isEmpty();
    }

    @Test
    void blockedRunsRoundOutwardAndClipToTheHorizon() {
        Horizon h = horizon("2026-01-05T00:00:00Z", "2026-01-06T00:00:00Z", 3600);
        List<DiagnosticIssue> warnings = new ArrayList<>();
        int[] runs = CalendarExpander.blockedRuns(List.of(window("2026-01-05T10:15:00Z", "2026-01-05T10:45:00Z"),
                window("2026-01-04T12:00:00Z", "2026-01-05T01:00:00Z")), h, warnings, "resources[0].unavailable");
        assertThat(runs).containsExactly(0, 1, 10, 11);
        assertThat(warnings).extracting(DiagnosticIssue::code).contains("CALENDAR_ROUNDED");
        assertThat(warnings.get(0).path()).startsWith("resources[0].unavailable[");
    }
}

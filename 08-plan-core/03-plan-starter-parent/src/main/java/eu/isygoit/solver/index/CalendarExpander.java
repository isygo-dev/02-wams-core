package eu.isygoit.solver.index;

import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.RecurringWindow;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.exception.LimitExceededException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Expands a {@link Calendar} into sorted, merged, half-open availability runs in TICKS (design 1.1, 2.2, 5.4 step 2).
 * <p>
 * Semantics: {@code availability = (windows UNION expand(recurring)) MINUS (exceptions UNION expand(breaks))}; a calendar
 * with neither windows nor recurring rules is "always available" minus exceptions and breaks. Rounding follows the
 * design: availability starts round UP and ends round DOWN, unavailability starts round DOWN and ends round UP
 * (availability only ever shrinks), with a {@code CALENDAR_ROUNDED} warning. Explicit availability windows outside the
 * horizon are clipped with an {@code OUT_OF_HORIZON_CLIPPED} warning.
 * <p>
 * Recurring windows are expanded in the calendar time zone with {@code ZonedDateTime.of(LocalDate, LocalTime, zone)}:
 * a nonexistent local time (DST gap) moves forward, an ambiguous one (DST overlap) takes the earlier offset. The
 * {@code days} of a recurring window name the local day on which the window STARTS; {@code to <= from} crosses
 * midnight ({@code to == from} is 24 hours); {@code validFrom/validTo} bound the start date, inclusive.
 * <p>
 * Pure static functions on immutable inputs (thread-safe). Complexity O(windows + days in horizon x recurring rules).
 * Warning paths are relative to the calendar ({@code windows[2]}, {@code recurring[0]}); callers prepend their own.
 */
public final class CalendarExpander {

    private static final String WARNING = "WARNING";

    private CalendarExpander() {
    }

    /**
     * Expands one calendar.
     *
     * @param calendar    the calendar
     * @param horizon     the problem horizon (tick 0 = horizon start)
     * @param defaultZone zone used when {@code calendar.timezone()} is null (the problem time zone)
     * @param warnings    receives {@code CALENDAR_ROUNDED} / {@code OUT_OF_HORIZON_CLIPPED} warnings, may be {@code null}
     * @return sorted merged half-open runs {@code [lo0,hi0,lo1,hi1,...)} in ticks inside {@code [0, horizonTicks]}
     * @throws IllegalArgumentException when the calendar time zone is not a valid IANA id
     */
    public static int[] expand(Calendar calendar, Horizon horizon, ZoneId defaultZone, List<DiagnosticIssue> warnings) {
        return expand(calendar, horizon, defaultZone, warnings, Integer.MAX_VALUE);
    }

    /**
     * Expands one calendar and enforces a run budget.
     *
     * @param maxRuns maximum number of generated or resulting runs
     * @throws LimitExceededException ({@code max-calendar-runs}) when the budget is exceeded
     * @see #expand(Calendar, Horizon, ZoneId, List)
     */
    public static int[] expand(Calendar calendar, Horizon horizon, ZoneId defaultZone,
                               List<DiagnosticIssue> warnings, int maxRuns) {
        ZoneId zone = zoneOf(calendar, defaultZone);
        int h = (int) horizon.horizonTicks();
        int[] availability;
        if (calendar.windows().isEmpty() && calendar.recurring().isEmpty()) {
            availability = new int[]{0, h};
        } else {
            IntList pairs = new IntList();
            for (int i = 0; i < calendar.windows().size(); i++) {
                addExplicit(pairs, calendar.windows().get(i), horizon, true, warnings, "windows[" + i + "]", h);
                guard(pairs, maxRuns);
            }
            for (int i = 0; i < calendar.recurring().size(); i++) {
                addRecurring(pairs, calendar.recurring().get(i), horizon, zone, true, warnings,
                        "recurring[" + i + "]", h, maxRuns);
            }
            availability = normalize(pairs.toArray());
        }
        IntList blocked = new IntList();
        for (int i = 0; i < calendar.exceptions().size(); i++) {
            addExplicit(blocked, calendar.exceptions().get(i), horizon, false, warnings, "exceptions[" + i + "]", h);
            guard(blocked, maxRuns);
        }
        for (int i = 0; i < calendar.breaks().size(); i++) {
            addRecurring(blocked, calendar.breaks().get(i), horizon, zone, false, warnings,
                    "breaks[" + i + "]", h, maxRuns);
        }
        int[] result = subtract(availability, normalize(blocked.toArray()));
        if (result.length / 2 > maxRuns) {
            throw new LimitExceededException("max-calendar-runs", result.length / 2L, maxRuns);
        }
        return result;
    }

    /**
     * Converts explicit blocked intervals (unavailability: start rounds down, end rounds up) into runs.
     *
     * @param windows  blocked intervals (for example {@code Resource.unavailable})
     * @param horizon  the problem horizon
     * @param warnings receives {@code CALENDAR_ROUNDED} warnings, may be {@code null}
     * @param path     path prefix for warnings (for example {@code resources[3].unavailable})
     * @return sorted merged half-open runs clipped to the horizon
     */
    public static int[] blockedRuns(List<TimeWindow> windows, Horizon horizon, List<DiagnosticIssue> warnings,
                                    String path) {
        IntList pairs = new IntList();
        int h = (int) horizon.horizonTicks();
        for (int i = 0; i < windows.size(); i++) {
            addExplicit(pairs, windows.get(i), horizon, false, warnings, path + "[" + i + "]", h);
        }
        return normalize(pairs.toArray());
    }

    /**
     * Subtracts blocked runs from availability runs.
     *
     * @param runs   sorted merged half-open runs
     * @param blocks sorted merged half-open runs
     * @return {@code runs MINUS blocks}, sorted and merged
     */
    public static int[] subtract(int[] runs, int[] blocks) {
        if (runs.length == 0 || blocks.length == 0) {
            return runs;
        }
        IntList out = new IntList(runs.length);
        int nb = blocks.length / 2;
        int j = 0;
        for (int i = 0; i < runs.length; i += 2) {
            int cur = runs[i];
            int end = runs[i + 1];
            while (j < nb && blocks[2 * j + 1] <= cur) {
                j++;
            }
            int k = j;
            while (cur < end) {
                if (k >= nb || blocks[2 * k] >= end) {
                    out.add(cur);
                    out.add(end);
                    break;
                }
                if (blocks[2 * k] > cur) {
                    out.add(cur);
                    out.add(blocks[2 * k]);
                }
                cur = Math.max(cur, blocks[2 * k + 1]);
                k++;
            }
        }
        return out.toArray();
    }

    /**
     * Intersects two run lists.
     *
     * @param a sorted merged half-open runs
     * @param b sorted merged half-open runs
     * @return the common runs, sorted and merged
     */
    public static int[] intersect(int[] a, int[] b) {
        IntList out = new IntList(Math.min(a.length, b.length));
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            int lo = Math.max(a[i], b[j]);
            int hi = Math.min(a[i + 1], b[j + 1]);
            if (lo < hi) {
                out.add(lo);
                out.add(hi);
            }
            if (a[i + 1] < b[j + 1]) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return out.toArray();
    }

    /**
     * Sorts half-open pairs and merges overlapping or touching ones.
     *
     * @param pairs {@code [lo0,hi0,lo1,hi1,...)}, any order, {@code 0 <= lo < hi}
     * @return sorted merged runs
     */
    public static int[] normalize(int[] pairs) {
        int n = pairs.length / 2;
        if (n == 0) {
            return new int[0];
        }
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = (((long) pairs[2 * i]) << 32) | (pairs[2 * i + 1] & 0xFFFFFFFFL);
        }
        Arrays.sort(keys);
        IntList out = new IntList(pairs.length);
        int curLo = (int) (keys[0] >>> 32);
        int curHi = (int) keys[0];
        for (int i = 1; i < n; i++) {
            int lo = (int) (keys[i] >>> 32);
            int hi = (int) keys[i];
            if (lo <= curHi) {
                curHi = Math.max(curHi, hi);
            } else {
                out.add(curLo);
                out.add(curHi);
                curLo = lo;
                curHi = hi;
            }
        }
        out.add(curLo);
        out.add(curHi);
        return out.toArray();
    }

    // ------------------------------------------------------------------------------------------ internals

    private static ZoneId zoneOf(Calendar calendar, ZoneId defaultZone) {
        if (calendar.timezone() == null || calendar.timezone().isBlank()) {
            return defaultZone == null ? ZoneId.of("UTC") : defaultZone;
        }
        try {
            return ZoneId.of(calendar.timezone());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("calendar '" + calendar.id() + "' has an invalid timezone '"
                    + calendar.timezone() + "'", e);
        }
    }

    private static void guard(IntList pairs, int maxRuns) {
        if (pairs.size() / 2 > maxRuns) {
            throw new LimitExceededException("max-calendar-runs", pairs.size() / 2L, maxRuns);
        }
    }

    private static void addExplicit(IntList out, TimeWindow w, Horizon horizon, boolean availability,
                                    List<DiagnosticIssue> warnings, String path, int h) {
        if (w == null || w.start() == null || w.end() == null || !w.start().isBefore(w.end())) {
            return;
        }
        int lo = availability ? horizon.ceilTick(w.start()) : horizon.floorTick(w.start());
        int hi = availability ? horizon.floorTick(w.end()) : horizon.ceilTick(w.end());
        if (!horizon.isAligned(w.start()) || !horizon.isAligned(w.end())) {
            warn(warnings, "CALENDAR_ROUNDED", path,
                    "window bounds are not tick aligned and were rounded (availability shrinks, unavailability grows)");
        }
        if (availability && (w.start().isBefore(horizon.start()) || w.end().isAfter(horizon.end()))) {
            warn(warnings, "OUT_OF_HORIZON_CLIPPED", path, "availability window exceeds the horizon and was clipped");
        }
        lo = Math.max(0, lo);
        hi = Math.min(h, hi);
        if (lo < hi) {
            out.add(lo);
            out.add(hi);
        }
    }

    private static void addRecurring(IntList out, RecurringWindow rw, Horizon horizon, ZoneId zone,
                                     boolean availability, List<DiagnosticIssue> warnings, String path, int h,
                                     int maxRuns) {
        if (rw == null || rw.from() == null || rw.to() == null) {
            return;
        }
        boolean crossesMidnight = !rw.to().isAfter(rw.from());
        LocalDate first = horizon.start().atZone(zone).toLocalDate().minusDays(1);
        LocalDate last = horizon.end().atZone(zone).toLocalDate();
        boolean warned = false;
        int counter = 0;
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            if (rw.validFrom() != null && d.isBefore(rw.validFrom())) {
                continue;
            }
            if (rw.validTo() != null && d.isAfter(rw.validTo())) {
                break;
            }
            if (!rw.days().isEmpty() && !rw.days().contains(d.getDayOfWeek())) {
                continue;
            }
            ZonedDateTime start = ZonedDateTime.of(d, rw.from(), zone);
            ZonedDateTime end = ZonedDateTime.of(crossesMidnight ? d.plusDays(1) : d, rw.to(), zone);
            Instant si = start.toInstant();
            Instant ei = end.toInstant();
            if (!si.isBefore(ei) || !ei.isAfter(horizon.start()) || !si.isBefore(horizon.end())) {
                continue;
            }
            if (!warned && (!horizon.isAligned(si) || !horizon.isAligned(ei))) {
                warned = true;
                warn(warnings, "CALENDAR_ROUNDED", path,
                        "recurring window is not tick aligned and was rounded (availability shrinks, unavailability grows)");
            }
            int lo = Math.max(0, availability ? horizon.ceilTick(si) : horizon.floorTick(si));
            int hi = Math.min(h, availability ? horizon.floorTick(ei) : horizon.ceilTick(ei));
            if (lo < hi) {
                out.add(lo);
                out.add(hi);
                if ((++counter & 1023) == 0) {
                    guard(out, maxRuns);
                }
            }
        }
        guard(out, maxRuns);
    }

    private static void warn(List<DiagnosticIssue> warnings, String code, String path, String message) {
        if (warnings != null) {
            warnings.add(new DiagnosticIssue(code, WARNING, path, message));
        }
    }
}

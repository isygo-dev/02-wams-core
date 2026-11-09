package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.params.PeriodSpec;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;

/**
 * Static helper that tiles the horizon into period buckets (design 2.5 {@code PeriodSpec}, 6.5.7 / 6.5.9 / 6.5.15) and
 * maps ticks to buckets. Shared by the CP-SAT side ({@code BucketGrids}) and by the independent checkers, so that the model
 * and the validator attribute a task to EXACTLY the same period (design D-14: the period in which the task STARTS).
 * <p>
 * Pure functions on immutable inputs, thread-safe. Units: all results are integer ticks of the {@link Horizon} (tick 0 =
 * horizon start); buckets are half-open {@code [lo[k], hi[k])}, ascending, non-empty, and tile {@code [0, horizonTicks)}
 * without gap (the first and last bucket may be partial).
 * <ul>
 *   <li>{@code TICKS}: fixed width {@code spec.ticks()}, the last bucket may be shorter.</li>
 *   <li>{@code DAY}: calendar days of the given zone (local midnight to local midnight). DST aware: a boundary is the
 *       instant {@code ZonedDateTime.of(date, 00:00, zone)} (a nonexistent midnight moves forward), converted with
 *       {@link Horizon#ceilTick}, so a 23-hour or 25-hour day simply has fewer or more ticks.</li>
 *   <li>{@code WEEK}: as DAY, weeks start on Monday.</li>
 * </ul>
 */
public final class PeriodBuckets {

    private PeriodBuckets() {
    }

    /**
     * Tiles the horizon.
     *
     * @param horizon the horizon
     * @param zone    zone for DAY / WEEK (ignored for TICKS); null means UTC
     * @param spec    the period; TICKS needs {@code ticks >= 1}
     * @return {@code {lo, hi}}: two arrays of equal length (at least 1)
     * @throws IllegalArgumentException when the spec is incomplete
     */
    public static int[][] tile(Horizon horizon, ZoneId zone, PeriodSpec spec) {
        if (horizon == null || spec == null || spec.unit() == null) {
            throw new IllegalArgumentException("period unit is required");
        }
        long horizonTicks = horizon.horizonTicks();
        if (horizonTicks > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("horizon too large");
        }
        int h = (int) horizonTicks;
        ZoneId z = zone == null ? ZoneId.of("UTC") : zone;
        int[] cuts;
        switch (spec.unit()) {
            case TICKS -> {
                if (spec.ticks() == null || spec.ticks() < 1) {
                    throw new IllegalArgumentException("period.ticks must be >= 1 for unit TICKS");
                }
                long n = spec.ticks();
                int count = (int) ((h + n - 1) / n);
                cuts = new int[count + 1];
                for (int k = 0; k < count; k++) {
                    cuts[k] = (int) (k * n);
                }
                cuts[count] = h;
            }
            case DAY, WEEK -> cuts = calendarCuts(horizon, z, spec.unit() == PeriodSpec.Unit.WEEK, h);
            default -> throw new IllegalArgumentException("unknown period unit " + spec.unit());
        }
        int count = cuts.length - 1;
        int[] lo = new int[count];
        int[] hi = new int[count];
        for (int k = 0; k < count; k++) {
            lo[k] = cuts[k];
            hi[k] = cuts[k + 1];
        }
        return new int[][]{lo, hi};
    }

    private static int[] calendarCuts(Horizon horizon, ZoneId zone, boolean week, int h) {
        ZonedDateTime startZoned = horizon.start().atZone(zone);
        LocalDate first = startZoned.toLocalDate();
        if (week) {
            first = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        int stepDays = week ? 7 : 1;
        int[] buffer = new int[16];
        int size = 0;
        buffer[size++] = 0;
        for (LocalDate d = first.plusDays(stepDays); ; d = d.plusDays(stepDays)) {
            Instant boundary = ZonedDateTime.of(d, LocalTime.MIDNIGHT, zone).toInstant();
            int tick = horizon.ceilTick(boundary);
            if (tick >= h) {
                break;
            }
            if (tick > buffer[size - 1]) {
                if (size == buffer.length) {
                    buffer = Arrays.copyOf(buffer, size * 2);
                }
                buffer[size++] = tick;
            }
        }
        if (size == buffer.length) {
            buffer = Arrays.copyOf(buffer, size + 1);
        }
        buffer[size++] = h;
        return Arrays.copyOf(buffer, size);
    }

    /**
     * Index of the bucket that contains {@code tick}.
     *
     * @param lo   bucket lower bounds (inclusive), ascending
     * @param hi   bucket upper bounds (exclusive)
     * @param tick the tick
     * @return the bucket index, or -1 when the tick is outside {@code [lo[0], hi[last])}
     */
    public static int bucketOf(int[] lo, int[] hi, int tick) {
        int n = lo.length;
        if (n == 0 || tick < lo[0] || tick >= hi[n - 1]) {
            return -1;
        }
        int a = 0;
        int b = n - 1;
        while (a < b) {
            int mid = (a + b + 1) >>> 1;
            if (lo[mid] <= tick) {
                a = mid;
            } else {
                b = mid - 1;
            }
        }
        return a;
    }

    /**
     * Buckets reachable by a task whose start lies in {@code [estStart, lstStart]} (inclusive ticks).
     *
     * @return {@code {firstBucket, lastBucket}} (inclusive), or null when the domain is empty or outside the grid
     */
    public static int[] bucketRange(int[] lo, int[] hi, int estStart, int lstStart) {
        if (estStart > lstStart) {
            return null;
        }
        int n = lo.length;
        if (n == 0) {
            return null;
        }
        int a = Math.max(estStart, lo[0]);
        int b = Math.min(lstStart, hi[n - 1] - 1);
        if (a > b) {
            return null;
        }
        return new int[]{bucketOf(lo, hi, a), bucketOf(lo, hi, b)};
    }

    /**
     * Stable identifier of a grid, used as cache key.
     *
     * @param spec the period
     * @param zone the zone (ignored for TICKS)
     * @return for example {@code "TICKS:12"} or {@code "DAY@Europe/Paris"}
     */
    public static String id(PeriodSpec spec, ZoneId zone) {
        if (spec.unit() == PeriodSpec.Unit.TICKS) {
            return "TICKS:" + spec.ticks();
        }
        return spec.unit().name() + "@" + (zone == null ? "UTC" : zone.getId());
    }
}

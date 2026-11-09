package eu.isygoit.dto.request;

import eu.isygoit.exception.TimeNotAlignedException;

import java.time.Duration;
import java.time.Instant;

/**
 * The planning horizon and the time grid of a problem.
 * <p>
 * Time is modelled as integer <b>ticks</b>: tick 0 is {@link #start()}, tick {@link #horizonTicks()} is
 * {@link #end()}, one tick lasts {@code tickSeconds} seconds. All durations and lags of the IR are in ticks, all
 * time POINTS of the request are ISO-8601 instants (UTC) converted through this class.
 * <p>
 * Invariants (violations throw {@link IllegalArgumentException}): {@code start}/{@code end} not null and
 * {@code end > start}; {@code tickSeconds} in {@code [1, 86400]} and a divisor of 86400 (a day boundary is always a
 * tick boundary); {@code end - start} a whole multiple of {@code tickSeconds}; {@link #horizonTicks()} at most
 * {@link #HARD_MAX_TICKS} (2^30). The configurable cap is {@code app.plan.limits.max-horizon-ticks}.
 * <pre>{@code
 * { "start": "2026-09-07T06:00:00Z", "end": "2026-09-08T08:15:00Z", "tickSeconds": 900 }   // 105 ticks
 * }</pre>
 *
 * @param start       instant of tick 0
 * @param end         instant of the last tick boundary (exclusive end of the horizon)
 * @param tickSeconds length of one tick in seconds
 */
public record Horizon(Instant start, Instant end, int tickSeconds) {

    /**
     * Largest allowed {@code tickSeconds} (one day).
     */
    public static final int MAX_TICK_SECONDS = 86_400;

    /**
     * Hard cap of {@link #horizonTicks()} (2^30) so that ticks always fit an {@code int} with room for arithmetic.
     */
    public static final long HARD_MAX_TICKS = 1L << 30;

    public Horizon {
        if (start == null || end == null) {
            throw new IllegalArgumentException("horizon.start and horizon.end are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("horizon.end must be after horizon.start");
        }
        if (tickSeconds < 1 || tickSeconds > MAX_TICK_SECONDS || MAX_TICK_SECONDS % tickSeconds != 0) {
            throw new IllegalArgumentException(
                    "horizon.tickSeconds must be between 1 and 86400 and divide 86400, got " + tickSeconds);
        }
        Duration length = Duration.between(start, end);
        if (length.getNano() != 0 || length.getSeconds() % tickSeconds != 0) {
            throw new IllegalArgumentException(
                    "horizon length must be a whole multiple of tickSeconds (" + tickSeconds + ")");
        }
        if (length.getSeconds() / tickSeconds > HARD_MAX_TICKS) {
            throw new IllegalArgumentException(
                    "horizon has more than " + HARD_MAX_TICKS + " ticks; use a coarser tickSeconds");
        }
    }

    /**
     * @return number of ticks of the horizon, {@code (end - start) / tickSeconds}
     */
    public long horizonTicks() {
        return Duration.between(start, end).getSeconds() / tickSeconds;
    }

    /**
     * @param t an instant (null gives {@code false})
     * @return {@code true} when {@code (t - start)} is a whole multiple of {@code tickSeconds} (any sign)
     */
    public boolean isAligned(Instant t) {
        if (t == null) {
            return false;
        }
        Duration d = Duration.between(start, t);
        return d.getNano() == 0 && Math.floorMod(d.getSeconds(), (long) tickSeconds) == 0;
    }

    /**
     * Exact conversion of an aligned instant inside {@code [start, end]}.
     *
     * @param t the instant
     * @return its tick, in {@code [0, horizonTicks]}
     * @throws TimeNotAlignedException (unchecked) when {@code t} is null, not aligned, or outside the horizon
     */
    public int toTick(Instant t) {
        if (t == null) {
            throw new TimeNotAlignedException(null, false, "time point is missing");
        }
        if (!isAligned(t)) {
            throw new TimeNotAlignedException(t, false,
                    "instant " + t + " is not on a " + tickSeconds + "s tick boundary of the horizon");
        }
        long tick = Duration.between(start, t).getSeconds() / tickSeconds;
        if (tick < 0 || tick > horizonTicks()) {
            throw new TimeNotAlignedException(t, true,
                    "instant " + t + " is outside the horizon [" + start + ", " + end + "]");
        }
        return (int) tick;
    }

    /**
     * Smallest tick {@code >= t} (used for calendar AVAILABILITY starts: availability only ever shrinks). The result
     * is NOT clipped to the horizon: it saturates at the {@code int} range and the caller clips.
     *
     * @param t the instant, any alignment, any position relative to the horizon
     * @return the ceiling tick (may be negative or larger than {@link #horizonTicks()})
     */
    public int ceilTick(Instant t) {
        Duration d = Duration.between(start, t);
        long seconds = d.getSeconds();
        long q = Math.floorDiv(seconds, (long) tickSeconds);
        boolean exact = Math.floorMod(seconds, (long) tickSeconds) == 0 && d.getNano() == 0;
        return saturate(exact ? q : q + 1);
    }

    /**
     * Largest tick {@code <= t} (used for calendar AVAILABILITY ends: availability only ever shrinks). The result
     * is NOT clipped to the horizon: it saturates at the {@code int} range and the caller clips.
     *
     * @param t the instant, any alignment, any position relative to the horizon
     * @return the floor tick (may be negative or larger than {@link #horizonTicks()})
     */
    public int floorTick(Instant t) {
        Duration d = Duration.between(start, t);
        return saturate(Math.floorDiv(d.getSeconds(), (long) tickSeconds));
    }

    /**
     * @param tick a tick (may be outside the horizon)
     * @return {@code start + tick * tickSeconds}
     */
    public Instant toInstant(long tick) {
        return start.plusSeconds(Math.multiplyExact(tick, (long) tickSeconds));
    }

    /**
     * Converts a duration to ticks.
     *
     * @param d a non-null duration that is a whole multiple of {@code tickSeconds}
     * @return {@code d / tickSeconds}
     * @throws IllegalArgumentException when {@code d} is not a multiple of the tick length
     * @throws ArithmeticException      when the tick count does not fit an {@code int}
     */
    public int ticks(Duration d) {
        if (d.getNano() != 0 || Math.floorMod(d.getSeconds(), (long) tickSeconds) != 0) {
            throw new IllegalArgumentException("duration " + d + " is not a multiple of " + tickSeconds + "s");
        }
        return Math.toIntExact(d.getSeconds() / tickSeconds);
    }

    /**
     * @param ticks a number of ticks
     * @return the corresponding duration
     */
    public Duration duration(long ticks) {
        return Duration.ofSeconds(Math.multiplyExact(ticks, (long) tickSeconds));
    }

    private static int saturate(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}

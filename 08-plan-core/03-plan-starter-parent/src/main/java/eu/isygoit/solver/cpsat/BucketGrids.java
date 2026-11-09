package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.solver.spi.PeriodBuckets;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Factory of {@link BucketGrid}s: tiles the horizon into DAY / WEEK / TICKS buckets (design 5.5 {@code CpModelContext.buckets},
 * 6.5.7, 6.5.9). Used by {@code CpModelContext.buckets(PeriodSpec)} (WP3, which caches the result per grid id) and by the
 * windowed contributors.
 * <p>
 * Delegates to {@link PeriodBuckets} so that the model and the independent checkers use the same tiling. DST aware in
 * {@code meta.timezone} (23-hour and 25-hour days). Stateless and thread-safe. Units: ticks of the horizon, buckets are
 * half-open {@code [lo[k], hi[k])}.
 */
public final class BucketGrids {

    private BucketGrids() {
    }

    /**
     * @param horizon the horizon
     * @param zone    zone for DAY / WEEK; null means UTC
     * @param period  the period spec
     * @return the grid (id = {@link PeriodBuckets#id})
     */
    public static BucketGrid of(Horizon horizon, ZoneId zone, PeriodSpec period) {
        int[][] t = PeriodBuckets.tile(horizon, zone, period);
        return new BucketGrid(t[0], t[1], PeriodBuckets.id(period, zone));
    }

    /**
     * @param horizon  the horizon
     * @param timezone IANA id, blank or invalid means UTC
     * @param period   the period spec
     * @return the grid
     */
    public static BucketGrid of(Horizon horizon, String timezone, PeriodSpec period) {
        return of(horizon, zoneOf(timezone), period);
    }

    /**
     * @param problem the normalized problem (horizon and {@code timezone} are read from it)
     * @param period  the period spec
     * @return the grid
     */
    public static BucketGrid of(PlanningProblem problem, PeriodSpec period) {
        return of(problem.horizon(), zoneOf(problem.timezone()), period);
    }

    /**
     * @param timezone IANA id
     * @return the zone, UTC when blank or invalid
     */
    public static ZoneId zoneOf(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}

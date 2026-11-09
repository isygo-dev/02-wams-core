package eu.isygoit.solver.family;

import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.PeriodBuckets;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ViolationSink;

import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Independent per-period load check shared by {@code max_load_per_period} and {@code max_tasks_per_resource}
 * (design 6.5.9, 6.5.15): for every scoped resource and every APPLICABLE bucket the load (task duration in ticks, or task
 * count) attributed to the bucket of the task START is compared with {@code max} and {@code min}.
 * <p>
 * Unit of a violation = the excess above max, or the deficit below min (ticks or tasks), one violation per (resource,
 * bucket). A bucket is applicable when some candidate of the resource can start in it
 * ({@link FamilySupport#reachableBuckets}), exactly the buckets the CP-SAT contributor models. Uses no solver. Stateless,
 * thread-safe.
 */
public final class PeriodLoadChecks {

    private PeriodLoadChecks() {
    }

    /**
     * Runs the check.
     *
     * @param ctx          check context
     * @param resources    scoped resource indices
     * @param period       the period; ignored when {@code wholeHorizon}
     * @param wholeHorizon true = one bucket covering the horizon
     * @param metric       TICKS or COUNT
     * @param max          upper bound or null
     * @param min          lower bound or null
     * @param sink         violation sink
     * @param maxCode      violation code for an excess
     * @param minCode      violation code for a deficit
     */
    public static void check(CheckContext ctx, int[] resources, PeriodSpec period, boolean wholeHorizon, LoadMetric metric,
                             Integer max, Integer min, ViolationSink sink, String maxCode, String minCode) {
        CandidateIndexView index = ctx.index();
        SolutionView solution = ctx.solution();
        int[] lo;
        int[] hi;
        if (wholeHorizon) {
            lo = new int[]{0};
            hi = new int[]{index.horizonTicks()};
        } else {
            int[][] tiles = PeriodBuckets.tile(index.horizon(), FamilySupport.zone(ctx.problem()), period);
            lo = tiles[0];
            hi = tiles[1];
        }
        Map<Integer, BitSet> reach = FamilySupport.reachableBuckets(index, resources, lo, hi);
        for (Map.Entry<Integer, BitSet> entry : reach.entrySet()) {
            int r = entry.getKey();
            BitSet buckets = entry.getValue();
            Map<Integer, Long> loads = new TreeMap<>();
            for (int t : solution.tasksOfResource(r)) {
                int k = wholeHorizon ? 0 : PeriodBuckets.bucketOf(lo, hi, solution.start(t));
                long value = metric == LoadMetric.COUNT ? 1L : (long) solution.end(t) - solution.start(t);
                loads.merge(k, value, Long::sum);
            }
            for (int k = buckets.nextSetBit(0); k >= 0; k = buckets.nextSetBit(k + 1)) {
                long load = loads.getOrDefault(k, 0L);
                String where = wholeHorizon ? "the horizon"
                        : "period [" + index.horizon().toInstant(lo[k]) + ", " + index.horizon().toInstant(hi[k]) + ")";
                if (max != null && load > max) {
                    sink.violation(null, index.resourceId(r), maxCode,
                            "resource " + index.resourceId(r) + " has " + load + " " + unit(metric) + " in " + where
                                    + ", maximum is " + max, load - max);
                }
                if (min != null && load < min) {
                    sink.violation(null, index.resourceId(r), minCode,
                            "resource " + index.resourceId(r) + " has only " + load + " " + unit(metric) + " in "
                                    + where + ", minimum is " + min, min - load);
                }
            }
        }
    }

    private static String unit(LoadMetric metric) {
        return metric == LoadMetric.COUNT ? "task(s)" : "tick(s)";
    }
}

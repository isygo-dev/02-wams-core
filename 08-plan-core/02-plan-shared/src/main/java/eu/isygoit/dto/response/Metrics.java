package eu.isygoit.dto.response;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Timing and statistics of a solve. The time fields are excluded from determinism comparisons.
 *
 * @param durationMillis             total wall time of the solve call (build + solve + post)
 * @param buildMillis                index and model construction
 * @param solveMillis                inside CpSolver.solve / Timefold solve
 * @param timeToFirstSolutionMillis  from solve start; null if no solution was found
 * @param workersUsed                number of workers used
 * @param model                      model size, may be null
 * @param search                     search statistics, may be null
 * @param tiers                      per-tier values, ascending tier
 */
public record Metrics(
        long durationMillis,
        long buildMillis,
        long solveMillis,
        Long timeToFirstSolutionMillis,
        int workersUsed,
        ModelStats model,
        SearchStats search,
        List<TierValue> tiers) {

    public Metrics {
        if (durationMillis < 0 || buildMillis < 0 || solveMillis < 0 || workersUsed < 0) {
            throw new IllegalArgumentException("Metrics must not be negative");
        }
        tiers = CollectionCopies.list(tiers);
    }
}

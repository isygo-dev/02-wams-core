package eu.isygoit.solver;

/**
 * Immutable progress event of a solve. Created by the engine, consumed by a {@link SolveListener}.
 *
 * @param elapsedMillis wall time since the start of the whole request, in milliseconds
 * @param solutionCount number of solutions found so far
 * @param objective     current objective value in scaled units, or {@code null} when none yet
 * @param bestBound     best bound in scaled units, or {@code null} when unknown (Timefold, weighted multi-tier)
 * @param gap           relative gap, or {@code null} when unknown
 * @param tier          lexicographic tier being solved (-2 relaxation, -1 auto selection, 0..7 user tiers)
 * @param phase         one of BUILD | PRESOLVE | SEARCH | TIER_FIXING | EXPLAIN | POST
 */
public record SolveProgress(
        long elapsedMillis,
        int solutionCount,
        Double objective,
        Double bestBound,
        Double gap,
        int tier,
        String phase) {
}

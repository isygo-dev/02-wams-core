package eu.isygoit.dto.response;

/**
 * Search statistics of the solve.
 *
 * @param branches       search branches
 * @param conflicts      search conflicts
 * @param solutionsFound number of improving solutions found
 * @param objective      last objective value of the LAST solved model (scaled units)
 * @param bestBound      best bound; null when no bound exists (Timefold) or for a weighted multi-tier solve
 * @param gap            relative gap {@code |obj-bound|/max(1,|obj|)}; null when no bound
 * @param tiersSolved    number of tiers solved
 */
public record SearchStats(
        long branches,
        long conflicts,
        int solutionsFound,
        Double objective,
        Double bestBound,
        Double gap,
        int tiersSolved) {
}

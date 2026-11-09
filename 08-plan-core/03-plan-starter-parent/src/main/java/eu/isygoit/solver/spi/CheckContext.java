package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.PlanningProblem;

/**
 * Everything a {@link ConstraintChecker} or {@link ObjectiveEvaluator} needs: problem, index and solution.
 * <p>
 * Implemented by {@code CheckContextImpl} (WP6), one per validation; used on the validating thread. Read-only.
 */
public interface CheckContext {

    /** @return the normalized problem */
    PlanningProblem problem();

    /** @return the read-only index of the problem (id/idx lookups, scope resolution, per-task data) */
    CandidateIndexView index();

    /** @return the solution under validation */
    SolutionView solution();

    /**
     * Converts a decimal input to its scaled integer ({@code ScaledMath.scale}: {@code Math.round(value*scale)} with
     * overflow guard); the single arithmetic shared with the engines.
     *
     * @throws eu.isygoit.exception.ObjectiveRangeException on overflow
     */
    long scaled(double value, int scale);
}

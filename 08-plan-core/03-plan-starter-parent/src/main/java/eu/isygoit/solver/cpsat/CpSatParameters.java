package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.SatParameters;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.SolverOptions;

/**
 * Translates {@link SolverOptions} into CP-SAT parameters (design 6.11 hints, 6.12 deterministic mode).
 * <ul>
 *   <li>Always: {@code max_time_in_seconds} (the remaining budget, build time already deducted), {@code random_seed},
 *   {@code num_workers}, {@code log_search_progress}, {@code relative_gap_limit} (only when requested for this solve).</li>
 *   <li>Hints present: {@code repair_hint = true}, {@code hint_conflict_limit = 10}.</li>
 *   <li>Deterministic: one worker stays single threaded; several workers use {@code interleave_search} with the same
 *   {@code num_workers}; the search is bounded by {@code max_deterministic_time = maxTimeSeconds * dtimePerSecond} AND the
 *   wall-clock safety net {@code max_time_in_seconds}.</li>
 * </ul>
 * Stateless utility; thread-safe. Units: seconds for CP-SAT times, milliseconds for the arguments.
 * OR-TOOLS-VERIFY: {@code SatParameters.Builder.setNumWorkers/setInterleaveSearch/setMaxDeterministicTime/setRepairHint/
 * setHintConflictLimit/setRelativeGapLimit} (verified against the 9.15.6755 jar signatures).
 */
public final class CpSatParameters {

    private static final int HINT_CONFLICT_LIMIT = 10;

    private CpSatParameters() {
    }

    /**
     * What {@link #apply} configured, for reporting.
     *
     * @param workers          the effective number of workers
     * @param deterministic    true when the reproducible parameters were applied
     * @param wallLimitSeconds the wall-clock limit that was set
     */
    public record Applied(int workers, boolean deterministic, double wallLimitSeconds) {
    }

    /**
     * Applies the parameters to a fresh solver.
     *
     * @param solver         the solver (parameters are set on {@code solver.getParameters()})
     * @param options        the request options
     * @param properties     the CP-SAT properties (deterministic time factor)
     * @param limits         the service limits (worker clamp), may be null
     * @param budgetMillis   wall-clock budget of THIS solve in milliseconds (>= 1)
     * @param hasHints       true when the model carries hints
     * @param gapTolerance   relative gap at which the search stops, {@code 0} to prove optimality
     * @return what was applied
     */
    public static Applied apply(CpSolver solver, SolverOptions options, CpSatProperties properties,
                                PlanLimitsProperties limits, long budgetMillis, boolean hasHints, double gapTolerance) {
        SatParameters.Builder parameters = solver.getParameters();
        int workers = options.numWorkers();
        if (limits != null) {
            workers = Math.min(workers, Math.max(1, limits.getMaxWorkers()));
        }
        workers = Math.max(1, workers);
        double wallSeconds = Math.max(0.001d, budgetMillis / 1000.0d);
        boolean deterministic = Boolean.TRUE.equals(options.deterministic());
        parameters.setMaxTimeInSeconds(wallSeconds)
                .setRandomSeed(Math.toIntExact(options.seed()))
                .setNumWorkers(workers)
                .setLogSearchProgress(Boolean.TRUE.equals(options.logSearch()));
        if (gapTolerance > 0.0d) {
            parameters.setRelativeGapLimit(gapTolerance);
        }
        if (hasHints) {
            parameters.setRepairHint(true).setHintConflictLimit(HINT_CONFLICT_LIMIT);
        }
        if (deterministic) {
            if (workers > 1) {
                parameters.setInterleaveSearch(true);
            }
            double perSecond = properties == null ? 1.0d : properties.getDtimePerSecond();
            parameters.setMaxDeterministicTime(Math.max(0.001d, wallSeconds * perSecond));
        }
        return new Applied(workers, deterministic, wallSeconds);
    }
}

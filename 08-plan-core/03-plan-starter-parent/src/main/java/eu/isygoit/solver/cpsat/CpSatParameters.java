package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.SatParameters;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.SolverOptions;

/**
 * Translates {@link SolverOptions} + {@link CpSatProperties} into CP-SAT parameters.
 * <p>
 * Precedence: per-request {@link SolverOptions} &gt; deployment {@link CpSatProperties} &gt; built-in fallback.
 * </p>
 *
 * <h2>What is configured</h2>
 * <ul>
 *   <li><b>Budget</b>: {@code max_time_in_seconds} = {@code budgetMillis / 1000} (build time already deducted by the caller).</li>
 *   <li><b>Workers</b>: {@code min(options.numWorkers, min(properties.workerCap, limits.maxWorkers))}; a
 *       {@code workerCap <= 0} disables the cap.</li>
 *   <li><b>Seed / logging</b>: {@code options.seed()} (the {@link SolverOptions} compact constructor guarantees
 *       non-null; the {@code properties.defaultSeed} fallback is purely defensive);
 *       logging on when {@code options.logSearch == true <b>or</b> properties.logSearchProgress == true}.</li>
 *   <li><b>Feasibility vs optimization</b>, resolved from {@link SolverOptions.FeasibilityMode}:
 *       <ul>
 *         <li>{@code FORCE_FEASIBLE} → stop at first solution;</li>
 *         <li>{@code FORCE_OPTIMIZE} → keep optimizing (treated as feasibility when the model has no objective,
 *             because there is nothing to optimize);</li>
 *         <li>{@code AUTO} → feasibility when {@code !hasObjective}, otherwise follow
 *             {@code properties.stopAfterFirstSolution}.</li>
 *       </ul>
 *       In the optimization branch a {@code relative_gap_limit} is set (explicit {@code gapTolerance}, else
 *       {@code properties.defaultRelativeGap}).</li>
 *   <li><b>Hints</b>: enabled when the model carries hints and {@code properties.repairHint}; the repair budget is
 *       {@code properties.hintConflictLimit}.</li>
 *   <li><b>Infeasibility cap</b>: request {@code maxConflicts > 0} wins; otherwise
 *       {@code properties.maxNumberOfConflicts}; applied only when the resolved value {@code > 0}.</li>
 *   <li><b>Deterministic mode</b>: {@code interleave_search} enabled when {@code workers > 1} and
 *       {@code properties.interleaveSearchOnDeterministic}; {@code max_deterministic_time = wallSeconds * dtimePerSecond}.</li>
 * </ul>
 *
 * <p>Stateless utility; thread-safe. Units: seconds for CP-SAT times, milliseconds for the arguments.</p>
 */
public final class CpSatParameters {

    /**
     * Used only when the caller passes {@code null} properties (tests, misconfiguration).
     */
    private static final CpSatProperties FALLBACK = new CpSatProperties();

    /**
     * Sentinel for "no worker cap"; guards against {@code workerCap <= 0} in YAML.
     */
    private static final int UNBOUNDED_WORKER_CAP = Integer.MAX_VALUE;

    private CpSatParameters() {
    }

    /**
     * Applies the parameters to a fresh solver.
     *
     * @param solver       the solver (parameters are set on {@code solver.getParameters()})
     * @param options      per-request options (non-null)
     * @param properties   deployment defaults (nullable; falls back to built-in defaults)
     * @param limits       service limits (worker clamp), nullable
     * @param budgetMillis wall-clock budget of THIS solve, in milliseconds ({@code >= 1})
     * @param hasHints     true when the model carries hints
     * @param gapTolerance caller-supplied relative gap ({@code > 0}); {@code 0} = use properties default
     * @param hasObjective true when the model calls {@code Minimize}/{@code Maximize}
     * @return what was applied, for reporting / tests
     */
    public static Applied apply(CpSolver solver,
                                SolverOptions options,
                                CpSatProperties properties,
                                PlanLimitsProperties limits,
                                long budgetMillis,
                                boolean hasHints,
                                double gapTolerance,
                                boolean hasObjective) {

        CpSatProperties props = (properties != null) ? properties : FALLBACK;
        SatParameters.Builder parameters = solver.getParameters();

        // ------------------------------------------------------------------
        // Workers: min(request, min(props.workerCap, limits.maxWorkers))
        // ------------------------------------------------------------------
        int effectiveCap = props.getWorkerCap() > 0 ? props.getWorkerCap() : UNBOUNDED_WORKER_CAP;
        if (limits != null) {
            effectiveCap = Math.min(effectiveCap, Math.max(1, limits.getMaxWorkers()));
        }
        int requestedWorkers = Math.max(1, options.numWorkers());
        int workers = Math.min(requestedWorkers, effectiveCap);

        // ------------------------------------------------------------------
        // Wall-clock budget
        // ------------------------------------------------------------------
        double wallSeconds = Math.max(0.001d, budgetMillis / 1000.0d);

        // ------------------------------------------------------------------
        // Logging: request OR deployment override
        // ------------------------------------------------------------------
        boolean logSearch = Boolean.TRUE.equals(options.logSearch()) || props.isLogSearchProgress();

        // ------------------------------------------------------------------
        // Seed: SolverOptions compact constructor guarantees non-null
        // (DEFAULT_SEED when unset); properties.defaultSeed is defensive only.
        // ------------------------------------------------------------------
        long seed = effectiveSeed(options, props);

        parameters
                .setMaxTimeInSeconds(wallSeconds)
                .setRandomSeed(Math.toIntExact(seed))
                .setNumWorkers(workers)
                .setLogSearchProgress(logSearch);

        // ------------------------------------------------------------------
        // Feasibility vs optimization — resolved from FeasibilityMode
        // ------------------------------------------------------------------
        boolean feasibilityOnly = switch (options.feasibilityMode()) {
            case FORCE_FEASIBLE -> true;
            case FORCE_OPTIMIZE -> !hasObjective; // nothing to optimize without an objective
            case AUTO -> !hasObjective || props.isStopAfterFirstSolution();
        };

        if (feasibilityOnly) {
            parameters.setStopAfterFirstSolution(true);
        } else {
            double gap = gapTolerance > 0.0d ? gapTolerance : props.getDefaultRelativeGap();
            if (gap > 0.0d) {
                parameters.setRelativeGapLimit(gap);
            }
        }

        // ------------------------------------------------------------------
        // Hints: repair path controlled by properties
        // ------------------------------------------------------------------
        if (hasHints && props.isRepairHint()) {
            int hintLimit = Math.max(1, props.getHintConflictLimit());
            parameters.setRepairHint(true).setHintConflictLimit(hintLimit);
        }

        // ------------------------------------------------------------------
        // Fast infeasibility detection
        // 0 sentinel on the request means "defer to properties.maxNumberOfConflicts".
        // ------------------------------------------------------------------
        int requestCap = options.maxConflicts() != null ? options.maxConflicts() : 0;
        Integer appliedCap = requestCap > 0
                ? Integer.valueOf(requestCap)
                : props.getMaxNumberOfConflicts();
        if (appliedCap != null && appliedCap > 0) {
            parameters.setMaxNumberOfConflicts(appliedCap);
        }

        // ------------------------------------------------------------------
        // Deterministic mode
        // ------------------------------------------------------------------
        boolean deterministic = Boolean.TRUE.equals(options.deterministic());
        if (deterministic) {
            if (workers > 1 && props.isInterleaveSearchOnDeterministic()) {
                parameters.setInterleaveSearch(true);
            }
            double perSecond = props.getDtimePerSecond();
            if (perSecond <= 0.0d) {
                perSecond = 1.0d;
            }
            parameters.setMaxDeterministicTime(Math.max(0.001d, wallSeconds * perSecond));
        }

        return new Applied(workers, deterministic, wallSeconds, feasibilityOnly,
                logSearch, appliedCap, seed);
    }

    /**
     * Effective seed: {@link SolverOptions#seed()} when set (the compact constructor guarantees non-null),
     * deployment default otherwise. Kept defensive for hand-built / reflective {@link SolverOptions} instances.
     */
    private static long effectiveSeed(SolverOptions options, CpSatProperties props) {
        Long requested = options.seed();
        return (requested != null) ? requested : props.getDefaultSeed();
    }

    /**
     * What {@link #apply} configured, for reporting / tests.
     *
     * @param workers              effective worker count
     * @param deterministic        true when the reproducible parameters were applied
     * @param wallLimitSeconds     wall-clock limit that was set
     * @param feasibilityOnly      true when {@code stop_after_first_solution} was set
     * @param logSearch            true when solver search logging is on
     * @param maxNumberOfConflicts applied conflict cap, or {@code null} when unbounded
     * @param seed                 effective random seed
     */
    public record Applied(int workers,
                          boolean deterministic,
                          double wallLimitSeconds,
                          boolean feasibilityOnly,
                          boolean logSearch,
                          Integer maxNumberOfConflicts,
                          long seed) {
    }
}
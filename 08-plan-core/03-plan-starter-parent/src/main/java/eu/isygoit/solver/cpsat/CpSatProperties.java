package eu.isygoit.solver.cpsat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs of the CP-SAT engine (prefix {@code app.plan.cpsat}, design 6.2 / 6.7 / 6.11 / 6.12).
 * <p>
 * Registered as a bean through {@code @EnableConfigurationProperties} in {@code PlanningEngineConfiguration} (WP7), exactly
 * like {@code PlanLimitsProperties}; every value has a safe default so the service starts without any configuration.
 * Read-only after binding; safe to share between threads. Time values are milliseconds, counts are plain numbers.
 * <p>
 * The first block controls model shaping and callbacks; the second block controls solver behaviour and is
 * meant to be tuned per environment (feasibility-first in production, optimization-friendly in benchmarks).
 */
@Data
@ConfigurationProperties(prefix = "app.plan.cpsat")
public class CpSatProperties {

    // ---------------------------------------------------------------------
    // Model shaping / callbacks (existing)
    // ---------------------------------------------------------------------

    /**
     * Gives CP-SAT variables human readable names (memory cost proportional to the number of candidates). Names are also
     * generated when {@code solverOptions.logSearch} is true.
     */
    private boolean namedVariables = false;

    /**
     * Maximum number of start choices of a slot-mode task for which one literal per allowed start is created; larger
     * domains fall back to the plain start domain without choice literals.
     */
    private int maxStartChoices = 256;

    /**
     * Kill switch of the symmetry breaking module (design 6.7 / R-11).
     */
    private boolean symmetryBreaking = true;

    /**
     * Deterministic time (CP-SAT {@code max_deterministic_time}) granted per second of {@code maxTimeSeconds} when
     * {@code solverOptions.deterministic} is true (design 6.12).
     */
    private double dtimePerSecond = 1.0;

    /**
     * Minimum interval between two {@code SolveListener.onProgress} events of the solution callback.
     */
    private long progressIntervalMillis = 1000L;

    /**
     * Minimum interval between two {@code SolveListener.onIncumbent} snapshots of the solution callback.
     */
    private long incumbentIntervalMillis = 2000L;

    // ---------------------------------------------------------------------
    // Solver behaviour (performance tuning)
    // ---------------------------------------------------------------------

    /**
     * Upper bound on the number of solver workers. OR-Tools' own guidance is that 8 threads is the portfolio-diversity
     * sweet spot for timetabling; raising it above 8 often slows wall-clock solve time because worker communication
     * dominates. The effective worker count is {@code min(solverOptions.numWorkers, min(this, limits.maxWorkers))}.
     * <p>
     * Default: {@code 8}. Set to a large value (e.g. {@code 2147483647}) to disable the cap and defer entirely to
     * {@code PlanLimitsProperties.maxWorkers}.
     */
    private int workerCap = 8;

    /**
     * Force-enable CP-SAT search logging regardless of {@code solverOptions.logSearch}. Should remain {@code false} in
     * production (log I/O and per-node formatting are measurable overhead); flip to {@code true} only during
     * diagnosis/benchmarks, or via an environment-specific profile.
     * <p>
     * Effective value: {@code solverOptions.logSearch || this}. Default: {@code false}.
     */
    private boolean logSearchProgress = false;

    /**
     * When the model carries an objective and the request did not explicitly ask for feasibility-only,
     * this default decides whether CP-SAT stops at the first feasible solution ({@code true}) or keeps optimizing
     * until the wall-clock budget is spent ({@code false}). Publication flows want {@code true}; offline
     * optimization jobs should override per request via {@code SolverOptions.feasibilityOnly=false}.
     * <p>
     * Default: {@code true}.
     */
    private boolean stopAfterFirstSolution = true;

    /**
     * Relative gap at which the solver stops when it <em>is</em> optimizing (either {@link #stopAfterFirstSolution}
     * is {@code false}, or the request passed {@code feasibilityOnly=false}). {@code 0} means "prove optimality" and
     * should only be used with a generous wall-clock budget.
     * <p>
     * Only consulted when the model has an objective and {@code gapTolerance} on the request is {@code <= 0}.
     * Default: {@code 0.01} (1 %).
     */
    private double defaultRelativeGap = 0.01d;

    /**
     * Repair budget for conflicting warm-start hints when {@link #repairHint} is enabled. A real timetable hint can
     * carry dozens of small inconsistencies (stale rooms, removed teachers); a budget of {@code 10} discards such a
     * hint almost immediately, while {@code 100–1000} gives CP-SAT room to repair it before falling back to a cold start.
     * <p>
     * Default: {@code 100}.
     */
    private int hintConflictLimit = 100;

    /**
     * Enables hint repair when the model carries hints. {@code false} disables hinting entirely (useful to benchmark
     * a cold-start baseline).
     * <p>
     * Default: {@code true}.
     */
    private boolean repairHint = true;

    /**
     * Hard cap on the number of conflicts before the solver aborts. Turns a hopeless infeasible model into a fast,
     * explicit failure instead of a wall-clock timeout, and pairs with {@code CpSolverStatus.INFEASIBLE} detection in
     * the generation service. {@code null} or {@code <= 0} means unbounded. The request-level
     * {@code SolverOptions.maxConflicts} overrides this when non-null.
     * <p>
     * Default: {@code null} (unbounded).
     */
    private Integer maxNumberOfConflicts = null;

    /**
     * Seed used when the request does not specify one (or when {@code SolverOptions.seed()} is {@code null}).
     * CP-SAT's own default is {@code 0}; keeping it here makes the value reproducible and configurable per environment.
     * <p>
     * Default: {@code 0}.
     */
    private long defaultSeed = 0L;

    /**
     * When deterministic mode ({@code solverOptions.deterministic=true}) runs with more than one worker, enables CP-SAT's
     * {@code interleave_search}. Interleaved search gives deterministic results across runs on the same machine while
     * still exploiting multiple threads; leaving it off makes multi-worker deterministic runs pointless.
     * <p>
     * Default: {@code true}.
     */
    private boolean interleaveSearchOnDeterministic = true;
}
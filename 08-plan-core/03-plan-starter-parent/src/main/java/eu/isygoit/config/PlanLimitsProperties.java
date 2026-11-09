package eu.isygoit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Safety limits of the planning service (prefix {@code app.plan.limits}).
 * <p>
 * Registered as a bean through {@code @EnableConfigurationProperties} (PlanningEngineConfiguration), exactly like the
 * pilot's AppProperties (no additional {@code @Component}). Every limit has a safe default so the service starts
 * without any configuration.
 */
@Data
@ConfigurationProperties(prefix = "app.plan.limits")
public class PlanLimitsProperties {

    /** Maximum number of tasks of one request. */
    private int maxTasks = 5000;

    /** Maximum number of resources of one request. */
    private int maxResources = 1000;

    /** Maximum number of slots of one request. */
    private int maxSlots = 2000;

    /**
     * Estimated number of Bool + Int variables of the whole model (candidates, optional presence, soft/relaxation
     * literals, family helpers), checked before any solver model is built (design 7.3).
     */
    private long maxVariables = 2_000_000L;

    /** Ticks of the horizon (hard cap 2^30 in {@code Horizon}); design 7.3 {@code max-horizon-ticks}. */
    private int maxHorizonTicks = 2_000_000;

    /** Surviving (task, requirement, resource) candidates; the main size guard of the v2 model. */
    private long maxCandidates = 500_000L;

    /** Estimated interval variables (candidate + master + fixed blocks). */
    private long maxIntervals = 1_000_000L;

    /** Structural precedence edges plus expanded {@code precedence} pairs. */
    private long maxPrecedences = 500_000L;

    /** Circuit arcs of the setup model after window pruning. */
    private long maxSetupArcs = 300_000L;

    /** Pair literals of pairwise fallbacks and soft pairwise families. */
    private long maxPairwise = 200_000L;

    /** Maximum number of resource requirements of one task. */
    private int maxRequirementsPerTask = 8;

    /** Maximum number of distinct lexicographic tiers of one request. */
    private int maxTiers = 8;

    /** Expanded availability runs of all calendars of one request. */
    private int maxCalendarRuns = 100_000;

    /** Estimated number of solver constraints. */
    private long maxConstraintInstances = 2_000_000L;

    /** Maximum number of constraints of one request. */
    private int maxConstraints = 20_000;

    /** Maximum (estimated or actual) size of a serialized request in bytes. */
    private long maxRequestBytes = 5_000_000L;

    /** Upper bound of {@code solverOptions.maxTimeSeconds}; larger values are rejected. */
    private int maxTimeSeconds = 3600;

    /** Upper bound of {@code solverOptions.numWorkers}; larger values are clamped. */
    private int maxWorkers = 8;

    /**
     * Maximum number of solves running at the same time on this node. When not set it is computed as
     * {@code max(1, availableProcessors / maxWorkers)}.
     */
    private Integer maxConcurrentSolves;

    /** How long a solve waits for a free solver slot before failing with SOLVER_BUSY. */
    private long solveQueueTimeoutSeconds = 300;

    /**
     * Timefold stops when the best score has not improved for this many seconds (0 disables it), so the first
     * attempt does not always consume the whole time budget.
     */
    private long timefoldUnimprovedSeconds = 30;

    /**
     * Resolves the effective number of concurrent solves.
     *
     * @return the configured value or {@code max(1, availableProcessors / maxWorkers)}
     */
    public int resolveMaxConcurrentSolves() {
        if (maxConcurrentSolves != null && maxConcurrentSolves > 0) {
            return maxConcurrentSolves;
        }
        return Math.max(1, Runtime.getRuntime().availableProcessors() / Math.max(1, maxWorkers));
    }
}

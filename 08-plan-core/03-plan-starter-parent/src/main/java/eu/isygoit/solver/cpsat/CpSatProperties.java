package eu.isygoit.solver.cpsat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs of the CP-SAT engine (prefix {@code app.plan.cpsat}, design 6.2 / 6.7 / 6.11 / 6.12).
 * <p>
 * Registered as a bean through {@code @EnableConfigurationProperties} in {@code PlanningEngineConfiguration} (WP7), exactly
 * like {@code PlanLimitsProperties}; every value has a safe default so the service starts without any configuration.
 * Read-only after binding; safe to share between threads. Time values are milliseconds, counts are plain numbers.
 */
@Data
@ConfigurationProperties(prefix = "app.plan.cpsat")
public class CpSatProperties {

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

    /** Kill switch of the symmetry breaking module (design 6.7 / R-11). */
    private boolean symmetryBreaking = true;

    /**
     * Deterministic time (CP-SAT {@code max_deterministic_time}) granted per second of {@code maxTimeSeconds} when
     * {@code solverOptions.deterministic} is true (design 6.12).
     */
    private double dtimePerSecond = 1.0;

    /** Minimum interval between two {@code SolveListener.onProgress} events of the solution callback. */
    private long progressIntervalMillis = 1000L;

    /** Minimum interval between two {@code SolveListener.onIncumbent} snapshots of the solution callback. */
    private long incumbentIntervalMillis = 2000L;
}

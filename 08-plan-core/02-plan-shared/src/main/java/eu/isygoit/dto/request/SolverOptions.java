package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.Locale;

/**
 * Solver selection and tuning. Every component is optional in JSON; the compact constructor materialises the
 * defaults, so a constructed instance never contains a null.
 * <pre>{@code
 * { "engine": "cp_sat", "maxTimeSeconds": 10, "numWorkers": 2, "deterministic": true }
 * }</pre>
 * Engine ids are canonical lower-case: {@code cp_sat} (aliases {@code cp-sat}, {@code cpsat}, any case, are
 * normalised here) and {@code timefold}. Any other value is kept as given (trimmed) and rejected later by the
 * solver registry (UNKNOWN_SOLVER).
 *
 * @param engine              default "cp_sat"; aliases "cp-sat", "cpsat" become "cp_sat"; "timefold"
 * @param maxTimeSeconds      default 30, {@code >= 1} (upper bound: limits.max-time-seconds, checked by the service)
 * @param numWorkers          default 8, 1..32 (clamped to limits.max-workers by the service)
 * @param seed                default 0, must fit an int
 * @param logSearch           default false
 * @param relaxationLevels    allow-list of relaxable constraint ids, empty = all authorized ones
 * @param deterministic       default false; true makes the run reproducible
 * @param objectiveScale      decimal to long multiplier, default 100, 1..1_000_000
 * @param gapTolerance        relative gap at which the search stops, default 0.0 (prove optimality), 0..1
 * @param lexicographicMode   default AUTO
 * @param explainInfeasibility default true: run the explanation pass when INFEASIBLE
 * @param explainTimeSeconds  default {@code min(10, 20% of maxTimeSeconds)}, {@code >= 1}
 * @param keepIncumbentOnStop default true: a stop request with an incumbent yields STOPPED_FEASIBLE
 * @param fallbackToTimefold default false; when true, retry with Timefold after CP-SAT returns UNKNOWN without a solution
 */
public record SolverOptions(
        String engine,
        Integer maxTimeSeconds,
        Integer numWorkers,
        Long seed,
        Boolean logSearch,
        java.util.List<String> relaxationLevels,
        Boolean deterministic,
        Integer objectiveScale,
        Double gapTolerance,
        LexicographicMode lexicographicMode,
        Boolean explainInfeasibility,
        Integer explainTimeSeconds,
        Boolean keepIncumbentOnStop,
        Boolean fallbackToTimefold) {

    /**
     * How the lexicographic tiers are solved.
     */
    public enum LexicographicMode { AUTO, WEIGHTED, SEQUENTIAL }

    public static final String ENGINE_CP_SAT = "cp_sat";
    public static final String ENGINE_TIMEFOLD = "timefold";
    public static final String DEFAULT_ENGINE = ENGINE_CP_SAT;
    public static final int DEFAULT_MAX_TIME_SECONDS = 30;
    public static final int DEFAULT_NUM_WORKERS = 8;
    public static final long DEFAULT_SEED = 0L;
    public static final boolean DEFAULT_LOG_SEARCH = false;
    public static final boolean DEFAULT_DETERMINISTIC = false;
    public static final int DEFAULT_OBJECTIVE_SCALE = 100;
    public static final int MAX_OBJECTIVE_SCALE = 1_000_000;
    public static final double DEFAULT_GAP_TOLERANCE = 0.0;
    public static final boolean DEFAULT_EXPLAIN_INFEASIBILITY = true;
    public static final boolean DEFAULT_KEEP_INCUMBENT_ON_STOP = true;
    /**
     * Upper bound of the default explain time.
     */
    public static final int DEFAULT_EXPLAIN_TIME_CAP_SECONDS = 10;

    public SolverOptions {
        engine = normalizeEngine(engine);
        maxTimeSeconds = maxTimeSeconds == null ? Integer.valueOf(DEFAULT_MAX_TIME_SECONDS) : maxTimeSeconds;
        numWorkers = numWorkers == null ? Integer.valueOf(DEFAULT_NUM_WORKERS) : numWorkers;
        seed = seed == null ? Long.valueOf(DEFAULT_SEED) : seed;
        logSearch = logSearch == null ? Boolean.valueOf(DEFAULT_LOG_SEARCH) : logSearch;
        deterministic = deterministic == null ? Boolean.valueOf(DEFAULT_DETERMINISTIC) : deterministic;
        objectiveScale = objectiveScale == null ? Integer.valueOf(DEFAULT_OBJECTIVE_SCALE) : objectiveScale;
        gapTolerance = gapTolerance == null ? Double.valueOf(DEFAULT_GAP_TOLERANCE) : gapTolerance;
        lexicographicMode = lexicographicMode == null ? LexicographicMode.AUTO : lexicographicMode;
        explainInfeasibility = explainInfeasibility == null
                ? Boolean.valueOf(DEFAULT_EXPLAIN_INFEASIBILITY) : explainInfeasibility;
        keepIncumbentOnStop = keepIncumbentOnStop == null
                ? Boolean.valueOf(DEFAULT_KEEP_INCUMBENT_ON_STOP) : keepIncumbentOnStop;
        fallbackToTimefold = fallbackToTimefold == null ? Boolean.FALSE : fallbackToTimefold;
        if (fallbackToTimefold && !ENGINE_CP_SAT.equals(engine)) {
            throw new IllegalArgumentException("fallbackToTimefold requires the cp_sat engine");
        }
        if (maxTimeSeconds < 1) {
            throw new IllegalArgumentException("maxTimeSeconds must be positive");
        }
        if (numWorkers < 1 || numWorkers > 32) {
            throw new IllegalArgumentException("numWorkers must be between 1 and 32");
        }
        if (seed.longValue() != (int) seed.longValue()) {
            throw new IllegalArgumentException("seed must fit a 32-bit integer");
        }
        if (objectiveScale < 1 || objectiveScale > MAX_OBJECTIVE_SCALE) {
            throw new IllegalArgumentException("objectiveScale must be between 1 and " + MAX_OBJECTIVE_SCALE);
        }
        if (!Double.isFinite(gapTolerance) || gapTolerance < 0.0 || gapTolerance > 1.0) {
            throw new IllegalArgumentException("gapTolerance must be between 0 and 1");
        }
        if (explainTimeSeconds == null) {
            explainTimeSeconds = Integer.valueOf(Math.max(1,
                    Math.min(DEFAULT_EXPLAIN_TIME_CAP_SECONDS, maxTimeSeconds / 5)));
        } else if (explainTimeSeconds < 1) {
            throw new IllegalArgumentException("explainTimeSeconds must be positive");
        }
        relaxationLevels = CollectionCopies.list(relaxationLevels);
    }

    /**
     * Backward-compatible constructor; fallback remains disabled unless explicitly requested.
     */
    public SolverOptions(String engine, Integer maxTimeSeconds, Integer numWorkers, Long seed, Boolean logSearch,
                         java.util.List<String> relaxationLevels, Boolean deterministic, Integer objectiveScale,
                         Double gapTolerance, LexicographicMode lexicographicMode, Boolean explainInfeasibility,
                         Integer explainTimeSeconds, Boolean keepIncumbentOnStop) {
        this(engine, maxTimeSeconds, numWorkers, seed, logSearch, relaxationLevels, deterministic, objectiveScale,
                gapTolerance, lexicographicMode, explainInfeasibility, explainTimeSeconds, keepIncumbentOnStop, null);
    }

    /**
     * Returns a copy with the requested primary engine and fallback strategy.
     */
    public SolverOptions withEngineAndFallback(String selectedEngine, boolean retryWithTimefold) {
        return new SolverOptions(selectedEngine, maxTimeSeconds, numWorkers, seed, logSearch, relaxationLevels,
                deterministic, objectiveScale, gapTolerance, lexicographicMode, explainInfeasibility,
                explainTimeSeconds, keepIncumbentOnStop, Boolean.valueOf(retryWithTimefold));
    }

    /**
     * @return options with every default applied
     */
    public static SolverOptions defaults() {
        return new SolverOptions(null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Canonical engine id: null/blank gives {@value #DEFAULT_ENGINE}; {@code cp_sat}, {@code cp-sat} and
     * {@code cpsat} (any case) give {@value #ENGINE_CP_SAT}; {@code timefold} (any case) gives
     * {@value #ENGINE_TIMEFOLD}; anything else is returned trimmed and unchanged.
     *
     * @param engine the requested engine id
     * @return the canonical id
     */
    public static String normalizeEngine(String engine) {
        if (engine == null || engine.isBlank()) {
            return DEFAULT_ENGINE;
        }
        String trimmed = engine.trim();
        String key = trimmed.toLowerCase(Locale.ROOT);
        switch (key) {
            case "cp_sat":
            case "cp-sat":
            case "cpsat":
                return ENGINE_CP_SAT;
            case "timefold":
                return ENGINE_TIMEFOLD;
            default:
                return trimmed;
        }
    }
}

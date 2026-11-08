package eu.isygoit.plan.domain;

import java.util.List;

public record SolverOptions(
        String engine,
        Integer maxTimeSeconds,
        Integer numWorkers,
        Long seed,
        Boolean logSearch,
        List<String> relaxationLevels) {

    public static final String DEFAULT_ENGINE = "cp_sat";
    public static final int DEFAULT_MAX_TIME_SECONDS = 30;
    public static final int DEFAULT_NUM_WORKERS = 8;
    public static final long DEFAULT_SEED = 0L;
    public static final boolean DEFAULT_LOG_SEARCH = false;

    public SolverOptions {
        engine = engine == null || engine.isBlank() ? DEFAULT_ENGINE : engine;
        maxTimeSeconds = maxTimeSeconds == null ? DEFAULT_MAX_TIME_SECONDS : maxTimeSeconds;
        numWorkers = numWorkers == null ? DEFAULT_NUM_WORKERS : numWorkers;
        seed = seed == null ? DEFAULT_SEED : seed;
        logSearch = logSearch == null ? DEFAULT_LOG_SEARCH : logSearch;
        if (maxTimeSeconds < 1) {
            throw new IllegalArgumentException("maxTimeSeconds must be positive");
        }
        if (numWorkers < 1 || numWorkers > 32) {
            throw new IllegalArgumentException("numWorkers must be between 1 and 32");
        }
        relaxationLevels = relaxationLevels == null ? List.of() : List.copyOf(relaxationLevels);
    }

    public static SolverOptions defaults() {
        return new SolverOptions(DEFAULT_ENGINE, DEFAULT_MAX_TIME_SECONDS, DEFAULT_NUM_WORKERS,
                DEFAULT_SEED, DEFAULT_LOG_SEARCH, List.of());
    }
}

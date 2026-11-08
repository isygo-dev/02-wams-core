package eu.isygoit.plan.domain;

import java.util.List;

public record SolverOptions(
        String engine,
        int maxTimeSeconds,
        int numWorkers,
        long seed,
        boolean logSearch,
        List<String> relaxationLevels) {

    public static final int DEFAULT_MAX_TIME_SECONDS = 30;
    public static final int DEFAULT_NUM_WORKERS = 8;

    public SolverOptions {
        engine = engine == null || engine.isBlank() ? "cp_sat" : engine;
        if (maxTimeSeconds < 1) {
            throw new IllegalArgumentException("maxTimeSeconds must be positive");
        }
        if (numWorkers < 1 || numWorkers > 32) {
            throw new IllegalArgumentException("numWorkers must be between 1 and 32");
        }
        relaxationLevels = relaxationLevels == null ? List.of() : List.copyOf(relaxationLevels);
    }

    public static SolverOptions defaults() {
        return new SolverOptions("cp_sat", DEFAULT_MAX_TIME_SECONDS, DEFAULT_NUM_WORKERS, 0L, false, List.of());
    }
}

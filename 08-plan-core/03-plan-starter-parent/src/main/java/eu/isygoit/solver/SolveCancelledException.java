package eu.isygoit.solver;

/**
 * Internal control-flow exception raised when a stop was requested (or the time budget ran out) while a solver model
 * was being built or while waiting for a solver slot. Engines and PlanningService translate it into an UNKNOWN
 * result; it never escapes to callers of {@code PlanningService.solve}.
 */
public final class SolveCancelledException extends RuntimeException {
    public static final String STOP_REQUESTED = "Solver stop was requested";
    public static final String TIME_LIMIT = "Solver time limit was reached";

    public SolveCancelledException(String message) {
        super(message, null, false, false);
    }

    /**
     * Checkpoint used inside long model-building loops.
     *
     * @param token      the cancellation token
     * @param startNanos {@link System#nanoTime()} taken when the solve started
     * @param maxSeconds the total time budget of the solve
     */
    public static void check(SolverCancellationToken token, long startNanos, long maxSeconds) {
        if (token.isCancellationRequested()) {
            throw new SolveCancelledException(STOP_REQUESTED);
        }
        if (System.nanoTime() - startNanos > maxSeconds * 1_000_000_000L) {
            throw new SolveCancelledException(TIME_LIMIT);
        }
    }
}

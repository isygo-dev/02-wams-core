package eu.isygoit.solver;

/**
 * Observer of a running solve (design 5.3). Implemented by {@code JobProcessor} (throttled PROGRESS log entries) or by
 * tests; called by the ENGINE, possibly from a solver callback thread, so implementations must be thread-safe and fast
 * (never block the search).
 */
public interface SolveListener {

    /** The listener that ignores everything. */
    SolveListener NOOP = new SolveListener() {
    };

    /** Progress event; the engine throttles calls to at most about one per second. */
    default void onProgress(SolveProgress progress) {
    }

    /** @return {@code true} when {@link #onIncumbent} must be called (snapshots are only built then) */
    default boolean wantsIncumbents() {
        return false;
    }

    /** New best solution; only called when {@link #wantsIncumbents()} is {@code true}. */
    default void onIncumbent(IncumbentSnapshot snapshot) {
    }
}

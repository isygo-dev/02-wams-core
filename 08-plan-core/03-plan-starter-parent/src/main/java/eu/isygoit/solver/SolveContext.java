package eu.isygoit.solver;

import eu.isygoit.dto.request.SolverOptions;

/**
 * Per-solve invocation context handed to {@link SolverEngine#solve}.
 * <p>
 * Created by {@code PlanningService} (one per solve); immutable. The cancellation token is thread-safe by itself, the
 * listener must be thread-safe when the engine calls it from solver callback threads.
 *
 * @param options      the effective solver options (limits already applied)
 * @param cancellation the existing cancellation token (never replaced); may be {@code null} in unit tests
 * @param listener     progress/incumbent listener; never null: {@link SolveListener#NOOP} when omitted
 * @param startNanos   {@code System.nanoTime()} at the START of the whole request (build time counts against the budget)
 */
public record SolveContext(
        SolverOptions options,
        SolverCancellationToken cancellation,
        SolveListener listener,
        long startNanos) {

    /** Budget used when the options carry no {@code maxTimeSeconds}. */
    public static final int DEFAULT_MAX_TIME_SECONDS = 30;

    public SolveContext {
        if (listener == null) {
            listener = SolveListener.NOOP;
        }
    }

    /**
     * Remaining wall-clock budget.
     *
     * @param nowNanos the current {@code System.nanoTime()}
     * @return {@code maxTimeSeconds*1000 - elapsedMillis}, never negative (0 = budget exhausted)
     */
    public long remainingMillis(long nowNanos) {
        int seconds = options == null || options.maxTimeSeconds() == null
                ? DEFAULT_MAX_TIME_SECONDS
                : options.maxTimeSeconds();
        long elapsedMillis = (nowNanos - startNanos) / 1_000_000L;
        return Math.max(0L, seconds * 1000L - elapsedMillis);
    }
}

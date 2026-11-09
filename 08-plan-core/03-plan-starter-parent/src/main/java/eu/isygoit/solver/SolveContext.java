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
 * @param attemptStartNanos start of this engine attempt; used only when an attempt-specific budget is set
 * @param attemptBudgetMillis optional per-attempt limit, or a negative value to use only the request budget
 */
public record SolveContext(
        SolverOptions options,
        SolverCancellationToken cancellation,
        SolveListener listener,
        long startNanos,
        long attemptStartNanos,
        long attemptBudgetMillis) {

    /** Budget used when the options carry no {@code maxTimeSeconds}. */
    public static final int DEFAULT_MAX_TIME_SECONDS = 30;

    public SolveContext {
        if (listener == null) {
            listener = SolveListener.NOOP;
        }
    }

    /**
     * Backward-compatible context using only the whole-request budget.
     */
    public SolveContext(SolverOptions options, SolverCancellationToken cancellation, SolveListener listener,
                        long startNanos) {
        this(options, cancellation, listener, startNanos, startNanos, -1L);
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
        long requestRemaining = Math.max(0L, seconds * 1000L - elapsedMillis);
        if (attemptBudgetMillis < 0L) {
            return requestRemaining;
        }
        long attemptElapsedMillis = (nowNanos - attemptStartNanos) / 1_000_000L;
        long attemptRemaining = Math.max(0L, attemptBudgetMillis - attemptElapsedMillis);
        return Math.min(requestRemaining, attemptRemaining);
    }
}

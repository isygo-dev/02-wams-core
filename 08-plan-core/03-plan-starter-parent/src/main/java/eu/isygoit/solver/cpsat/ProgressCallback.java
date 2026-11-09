package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpSolverSolutionCallback;
import eu.isygoit.solver.IncumbentSnapshot;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverCancellationToken;
import lombok.extern.slf4j.Slf4j;

/**
 * CP-SAT solution callback (design 6.11): counts the solutions, records the time to the first one, reads the objective and
 * the best bound, computes {@code gap = |obj - bound| / max(1, |obj|)} and forwards {@link SolveProgress} events to the
 * {@link SolveListener} at most once per {@code app.plan.cpsat.progress-interval-millis} (default 1 s). When the listener
 * {@link SolveListener#wantsIncumbents() wants incumbents} an {@link IncumbentSnapshot} is built at most once per
 * {@code incumbent-interval-millis} (default 2 s). The final solution is NOT copied here: it is read from the
 * {@code CpSolver} after the solve.
 * <p>
 * The callback also honours a stop request that arrived before the stop hook of the solver was active (a lost
 * {@code stopSearch}). Exceptions thrown by the listener are logged and swallowed: an exception must never cross the native
 * boundary. Thread-safety: invoked by CP-SAT on one thread at a time; the getters are meant to be read after the solve.
 * Units: elapsed times in milliseconds since the request start, objective values in scaled integers.
 */
@Slf4j
public final class ProgressCallback extends CpSolverSolutionCallback {

    private final CpModelContext ctx;
    private final SolveListener listener;
    private final SolverCancellationToken token;
    private final long startNanos;
    private final int tier;
    private final String phase;
    private final long progressIntervalNanos;
    private final long incumbentIntervalNanos;
    private final boolean hasObjective;

    private int solutions;
    private Long firstSolutionMillis;
    private double lastObjective;
    private double lastBound;
    private long lastProgressNanos;
    private long lastIncumbentNanos;

    /**
     * @param ctx          the model context (needed to read incumbent snapshots)
     * @param listener     the progress listener (never null)
     * @param token        the cancellation token
     * @param startNanos   {@code System.nanoTime()} at the start of the whole request
     * @param tier         the tier being optimised (reported in the events), or 0
     * @param phase        the phase label of the events (SEARCH, TIER_FIXING, ...)
     * @param properties   the CP-SAT properties (throttle intervals)
     * @param hasObjective true when the model has an objective (otherwise objective and bound are not reported)
     */
    public ProgressCallback(CpModelContext ctx, SolveListener listener, SolverCancellationToken token, long startNanos,
                            int tier, String phase, CpSatProperties properties, boolean hasObjective) {
        this.ctx = ctx;
        this.listener = listener == null ? SolveListener.NOOP : listener;
        this.token = token;
        this.startNanos = startNanos;
        this.tier = tier;
        this.phase = phase;
        this.progressIntervalNanos = Math.max(0L, properties.getProgressIntervalMillis()) * 1_000_000L;
        this.incumbentIntervalNanos = Math.max(0L, properties.getIncumbentIntervalMillis()) * 1_000_000L;
        this.hasObjective = hasObjective;
    }

    @Override
    public void onSolutionCallback() {
        try {
            long now = System.nanoTime();
            solutions++;
            long elapsedMillis = (now - startNanos) / 1_000_000L;
            if (firstSolutionMillis == null) {
                firstSolutionMillis = elapsedMillis;
            }
            lastObjective = hasObjective ? objectiveValue() : 0.0d;
            lastBound = hasObjective ? bestObjectiveBound() : 0.0d;
            if (solutions == 1 || now - lastProgressNanos >= progressIntervalNanos) {
                lastProgressNanos = now;
                Double objective = hasObjective ? lastObjective : null;
                Double bound = hasObjective ? lastBound : null;
                listener.onProgress(new SolveProgress(elapsedMillis, solutions, objective, bound,
                        hasObjective ? gap(lastObjective, lastBound) : null, tier, phase));
            }
            if (listener.wantsIncumbents() && now - lastIncumbentNanos >= incumbentIntervalNanos) {
                lastIncumbentNanos = now;
                listener.onIncumbent(new IncumbentSnapshot(elapsedMillis,
                        SolutionExtractor.assignments(ctx, SolutionExtractor.Values.of(this)), lastObjective));
            }
        } catch (RuntimeException failure) {
            log.warn("CP-SAT progress callback failed: {}", failure.toString());
        }
        if (token != null && token.isCancellationRequested()) {
            stopSearch();
        }
    }

    /**
     * @param objective the objective value
     * @param bound     the best bound
     * @return the relative gap {@code |objective - bound| / max(1, |objective|)}
     */
    public static double gap(double objective, double bound) {
        return Math.abs(objective - bound) / Math.max(1.0d, Math.abs(objective));
    }

    /** @return the number of solutions found by this solve */
    public int solutionCount() {
        return solutions;
    }

    /** @return the time of the first solution in milliseconds since the request start, or null when none was found */
    public Long firstSolutionMillis() {
        return firstSolutionMillis;
    }

    /** @return the objective of the last solution (0 without objective) */
    public double lastObjective() {
        return lastObjective;
    }

    /** @return the best bound seen at the last solution (0 without objective) */
    public double lastBound() {
        return lastBound;
    }
}

package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverCancellationToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the search of a {@link BuiltModel} (design 6.8, 6.11, 6.12): a single solve for the WEIGHTED / single-tier / no
 * objective case, or one solve per tier with fixing for SEQUENTIAL (time share of tier k = remaining / tiersLeft, at least
 * one second, leftover rolls over; an OPTIMAL tier is fixed with {@code expr == value}, a time-limited one with
 * {@code expr <= value}; the incumbent is handed to the next tier as a hint; {@code gapTolerance} applies to the last tier
 * only so earlier tiers keep their lexicographic guarantee).
 * <p>
 * The stop hook of the {@link SolverCancellationToken} ({@code onCancellation(solver::stopSearch)}) is registered right
 * before each {@code solve}, after a re-check of the token (a stop issued earlier is honoured without searching). Time
 * already consumed by index and model construction is deducted from the budget through
 * {@link SolveContext#remainingMillis(long)}. A cancelled or timed-out run still returns the best incumbent
 * ({@link Outcome#solver}).
 * <p>
 * Thread-safety: stateless apart from the immutable properties; one {@link #run} call per thread. Units: milliseconds,
 * ticks, scaled objective integers.
 */
public final class SolveRunner {

    private static final double GAP_EPSILON = 1e-9d;
    private static final long MIN_TIER_MILLIS = 1000L;

    private final CpSatProperties properties;

    /**
     * @param properties the CP-SAT properties (progress throttling, deterministic time factor)
     */
    public SolveRunner(CpSatProperties properties) {
        this.properties = properties == null ? new CpSatProperties() : properties;
    }

    /** Result of a run; read after {@link #run}. Public fields keep the value object trivial. */
    public static final class Outcome {
        /** Aggregate status: OPTIMAL (all tiers optimal), FEASIBLE, INFEASIBLE, MODEL_INVALID or UNKNOWN. */
        public CpSolverStatus status = CpSolverStatus.UNKNOWN;
        /** The solver that holds the final incumbent, {@code null} when there is no solution. */
        public CpSolver solver;
        /** A stop was requested (before or during the search). */
        public boolean cancelled;
        /** The time budget was exhausted before a (further) solve could start. */
        public boolean timeLimit;
        /** Deterministic mode was requested but the wall clock cut the search (results may differ between runs). */
        public boolean deterministicBroken;
        /** The number of workers actually configured. */
        public int workers;
        /** Total solutions found over all solves. */
        public int solutions;
        /** Milliseconds from the request start to the first solution, or null. */
        public Long firstSolutionMillis;
        /** Total branches over all solves. */
        public long branches;
        /** Total conflicts over all solves. */
        public long conflicts;
        /** Objective of the last solved model (scaled units), null without objective or solution. */
        public Double objective;
        /** Best bound of the last solved model, null when unknown (weighted multi-tier, no objective). */
        public Double bestBound;
        /** Relative gap of the last solved model, null when unknown. */
        public Double gap;
        /** Milliseconds spent inside the solver calls. */
        public long solveMillis;
        /** Number of tiers whose value was obtained. */
        public int tiersSolved;
        /** Per tier values and statuses (empty without objective terms). */
        public List<TierValue> tiers = new ArrayList<>();

        /** @return true when {@link #solver} holds a solution */
        public boolean hasSolution() {
            return solver != null && (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE);
        }
    }

    private record Attempt(CpSolverStatus status, CpSolver solver, ProgressCallback callback,
                           CpSatParameters.Applied applied, boolean cancelled, boolean timeUp) {
    }

    /**
     * Searches the model.
     *
     * @param built   the built SOLVE model
     * @param context the solve context (cancellation, listener, request start)
     * @return the outcome
     */
    public Outcome run(BuiltModel built, SolveContext context) {
        Outcome outcome = new Outcome();
        long started = System.nanoTime();
        TierObjectiveBuilder.Plan plan = built.plan();
        if (plan != null && plan.mode() == SolverOptions.LexicographicMode.SEQUENTIAL && plan.tierCount() > 1) {
            runSequential(built, context, plan, outcome);
        } else {
            runSingle(built, context, plan, outcome);
        }
        outcome.solveMillis = (System.nanoTime() - started) / 1_000_000L;
        return outcome;
    }

    // ------------------------------------------------------------------------------------------ single solve

    private void runSingle(BuiltModel built, SolveContext context, TierObjectiveBuilder.Plan plan, Outcome outcome) {
        CpModelContext ctx = built.context();
        SolverOptions options = ctx.options();
        double gapTolerance = options.gapTolerance();
        long remaining = context.remainingMillis(System.nanoTime());
        int tier = plan == null || plan.tierCount() == 0 ? 0 : plan.tiers()[plan.tierCount() - 1];
        Attempt attempt = attempt(built, context, tier, "SEARCH", remaining, gapTolerance, built.hasHints(),
                built.hasObjective());
        accumulate(outcome, attempt);
        if (attempt.solver() == null) {
            outcome.cancelled = attempt.cancelled();
            outcome.timeLimit = attempt.timeUp();
            outcome.status = CpSolverStatus.UNKNOWN;
            return;
        }
        outcome.cancelled = attempt.cancelled();
        outcome.status = attempt.status();
        outcome.solver = attempt.solver();
        if (!outcome.hasSolution()) {
            outcome.solver = null; // INFEASIBLE / UNKNOWN / MODEL_INVALID: there is no incumbent to read
            return;
        }
        boolean singleBound = built.hasObjective() && (plan == null || plan.tierCount() <= 1);
        if (built.hasObjective()) {
            double objective = attempt.solver().objectiveValue();
            outcome.objective = objective;
            if (singleBound) {
                double bound = attempt.solver().bestObjectiveBound();
                outcome.bestBound = bound;
                outcome.gap = ProgressCallback.gap(objective, bound);
            }
        } else if (attempt.status() == CpSolverStatus.OPTIMAL) {
            outcome.gap = 0.0d;
        }
        if (attempt.status() == CpSolverStatus.OPTIMAL && gapTolerance > 0.0d && outcome.gap != null
                && outcome.gap > GAP_EPSILON) {
            outcome.status = CpSolverStatus.FEASIBLE; // stopped on the gap limit: not proven optimal (Q-8)
        }
        if (plan != null) {
            String tierStatus = outcome.status == CpSolverStatus.OPTIMAL ? "OPTIMAL" : "FEASIBLE";
            for (int i = 0; i < plan.tierCount(); i++) {
                LinearExpr expr = built.tiers().expr(plan.tiers()[i]);
                Double bound = singleBound ? outcome.bestBound : null;
                outcome.tiers.add(new TierValue(plan.tiers()[i], attempt.solver().value(expr), bound, tierStatus));
            }
            outcome.tiersSolved = plan.tierCount();
        } else {
            outcome.tiersSolved = 0;
        }
    }

    // ----------------------------------------------------------------------------------------- sequential

    private void runSequential(BuiltModel built, SolveContext context, TierObjectiveBuilder.Plan plan, Outcome outcome) {
        CpModelContext ctx = built.context();
        CpModel model = ctx.model();
        double gapTolerance = ctx.options().gapTolerance();
        int[] tiers = plan.tiers();
        int count = tiers.length;
        String[] statuses = new String[count];
        java.util.Arrays.fill(statuses, "NOT_SOLVED");
        CpSolver best = null;
        boolean allOptimal = true;
        boolean hints = built.hasHints();
        Attempt last = null;
        for (int i = 0; i < count; i++) {
            if (ctx.token.isCancellationRequested()) {
                outcome.cancelled = true;
                break;
            }
            long remaining = context.remainingMillis(System.nanoTime());
            if (remaining <= 0) {
                outcome.timeLimit = true;
                break;
            }
            long budget = Math.min(remaining, Math.max(MIN_TIER_MILLIS, remaining / (count - i)));
            LinearExpr expr = built.tiers().expr(tiers[i]);
            model.clearObjective();
            model.minimize(expr);
            boolean lastTier = i == count - 1;
            Attempt attempt = attempt(built, context, tiers[i], "SEARCH", budget, lastTier ? gapTolerance : 0.0d, hints,
                    true);
            accumulate(outcome, attempt);
            if (attempt.solver() == null) {
                outcome.cancelled |= attempt.cancelled();
                outcome.timeLimit |= attempt.timeUp();
                break;
            }
            last = attempt;
            if (attempt.status() == CpSolverStatus.MODEL_INVALID) {
                outcome.status = CpSolverStatus.MODEL_INVALID;
                return;
            }
            if (attempt.status() == CpSolverStatus.INFEASIBLE) {
                if (best == null) {
                    outcome.status = CpSolverStatus.INFEASIBLE;
                    return;
                }
                break;
            }
            if (attempt.status() != CpSolverStatus.OPTIMAL && attempt.status() != CpSolverStatus.FEASIBLE) {
                outcome.cancelled |= attempt.cancelled();
                break;
            }
            best = attempt.solver();
            long value = best.value(expr);
            if (attempt.status() == CpSolverStatus.OPTIMAL) {
                model.addEquality(expr, value);
                statuses[i] = "OPTIMAL";
            } else {
                model.addLessOrEqual(expr, value);
                statuses[i] = "FEASIBLE";
                allOptimal = false;
            }
            outcome.tiersSolved++;
            context.listener().onProgress(new SolveProgress(
                    (System.nanoTime() - context.startNanos()) / 1_000_000L, outcome.solutions,
                    attempt.callback() == null ? null : attempt.callback().lastObjective(),
                    attempt.callback() == null ? null : attempt.callback().lastBound(),
                    null, tiers[i], "TIER_FIXING"));
            if (attempt.cancelled()) {
                outcome.cancelled = true;
                break;
            }
            if (!lastTier) {
                hints = HintApplier.applyFromSolver(ctx, best, built.relaxLiterals());
            }
        }
        if (best == null) {
            outcome.status = CpSolverStatus.UNKNOWN;
            return;
        }
        outcome.solver = best;
        boolean complete = outcome.tiersSolved == count && allOptimal && !outcome.cancelled;
        outcome.status = complete && gapReached(last, gapTolerance) ? CpSolverStatus.OPTIMAL : CpSolverStatus.FEASIBLE;
        for (int i = 0; i < count; i++) {
            LinearExpr expr = built.tiers().expr(tiers[i]);
            long value = best.value(expr);
            Double bound = "OPTIMAL".equals(statuses[i]) ? Double.valueOf(value) : null;
            outcome.tiers.add(new TierValue(tiers[i], value, bound, statuses[i]));
        }
        if (last != null && last.callback() != null) {
            outcome.objective = last.callback().lastObjective();
            outcome.bestBound = last.callback().lastBound();
            outcome.gap = ProgressCallback.gap(outcome.objective, outcome.bestBound);
        }
    }

    private static boolean gapReached(Attempt last, double gapTolerance) {
        if (gapTolerance <= 0.0d || last == null || last.callback() == null) {
            return true;
        }
        return ProgressCallback.gap(last.callback().lastObjective(), last.callback().lastBound()) <= GAP_EPSILON;
    }

    // ---------------------------------------------------------------------------------------------- attempt

    private Attempt attempt(BuiltModel built, SolveContext context, int tier, String phase, long budgetMillis,
                            double gapTolerance, boolean hints, boolean hasObjective) {
        CpModelContext ctx = built.context();
        SolverCancellationToken token = ctx.token;
        if (token.isCancellationRequested()) {
            return new Attempt(CpSolverStatus.UNKNOWN, null, null, null, true, false);
        }
        if (budgetMillis <= 0L) {
            return new Attempt(CpSolverStatus.UNKNOWN, null, null, null, false, true);
        }
        CpSolver solver = new CpSolver();
        CpSatParameters.Applied applied = CpSatParameters.apply(solver, ctx.options(), properties, ctx.limits,
                budgetMillis, hints, gapTolerance);
        ProgressCallback callback = new ProgressCallback(ctx, context.listener(), token, context.startNanos(), tier, phase,
                properties, hasObjective);
        CpSolverStatus status;
        try (SolverCancellationToken.Registration ignored = token.onCancellation(solver::stopSearch)) {
            // Re-check after the stop hook is registered: a stop issued during model building (or right before this
            // point) is honoured without starting the search.
            if (token.isCancellationRequested()) {
                return new Attempt(CpSolverStatus.UNKNOWN, null, null, applied, true, false);
            }
            status = solver.solve(built.model(), callback);
        }
        return new Attempt(status, solver, callback, applied, token.isCancellationRequested(), false);
    }

    private static void accumulate(Outcome outcome, Attempt attempt) {
        if (attempt.applied() != null && outcome.workers == 0) {
            outcome.workers = attempt.applied().workers();
        }
        if (attempt.callback() != null) {
            outcome.solutions += attempt.callback().solutionCount();
            if (outcome.firstSolutionMillis == null) {
                outcome.firstSolutionMillis = attempt.callback().firstSolutionMillis();
            }
        }
        if (attempt.solver() != null) {
            outcome.branches += attempt.solver().numBranches();
            outcome.conflicts += attempt.solver().numConflicts();
            if (attempt.applied() != null && attempt.applied().deterministic()
                    && attempt.status() != CpSolverStatus.OPTIMAL && attempt.status() != CpSolverStatus.INFEASIBLE
                    && attempt.solver().wallTime() >= attempt.applied().wallLimitSeconds() * 0.98d) {
                outcome.deterministicBroken = true;
            }
        }
    }
}

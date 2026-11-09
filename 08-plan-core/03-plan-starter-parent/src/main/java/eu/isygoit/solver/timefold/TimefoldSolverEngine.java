package eu.isygoit.solver.timefold;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.ModelStats;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.SearchStats;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.IncumbentSnapshot;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.registry.TimefoldContributorRegistry;
import eu.isygoit.util.ScaledMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Engine {@code timefold}: the documented SUBSET of Engine v2 (design 4.5 / 5.6) on Timefold 1.28.
 * <p>
 * Model: slot mode only. One {@link TimefoldTask} entity per task with ONE composite planning variable, the option
 * (a (candidate, slot) pair), over the shared immutable {@link SlotOptions} (built by {@link SlotOptionsBuilder}, filled
 * by the {@link TimefoldConstraintContributor}/{@link TimefoldObjectiveContributor} beans, then frozen). Score is
 * {@link HardSoftLongScore} (see {@link PlanConstraintProvider}).
 * <p>
 * Contract: never reports {@code OPTIMAL} (heuristic). A solution with hard score 0 is {@code FEASIBLE}, or
 * {@code STOPPED_FEASIBLE} when a stop was requested while it existed and {@code keepIncumbentOnStop} is set; a negative
 * hard score at termination is {@code UNKNOWN} (a heuristic cannot prove infeasibility); {@code INFEASIBLE} is never
 * returned by this engine (only the engine-neutral pre-solve analyzer proves it). Anything outside the subset that
 * reaches the engine (the {@code ProblemVerifier} normally rejects it first) fails with {@code InvalidRequestException}
 * ({@code ENGINE_UNSUPPORTED_*}, {@code OVERLAPPING_SLOTS}): it is never ignored.
 * <p>
 * Threading and determinism: always ONE move thread ({@code numWorkers} is ignored, {@code workersUsed = 1}); the seed
 * always applies; {@code deterministic} selects the REPRODUCIBLE environment mode (same seed and same stopping point give
 * the same plan; a wall-clock stop point cannot be reproduced exactly). Termination: spent limit = remaining budget,
 * unimproved limit = {@code app.plan.limits.timefold-unimproved-seconds} (0 disables), best-score limit = a trivial upper
 * bound of the score (stops early when it is reached). Cancellation: the {@link SolverCancellationToken} hook calls
 * {@code terminateEarly()}. Progress: best-solution events become {@link SolveProgress} (throttled to about one per
 * second) and, when the listener asks, {@link IncumbentSnapshot}s.
 * <p>
 * Spring singleton, stateless, thread-safe (one solve per call). Units: ticks in {@code Assignment.startTick/endTick}
 * and instants through the problem horizon; objective values are scaled longs ({@code TierValue.value} is the minimised
 * internal value: penalties minus rewards).
 */
@Component
public class TimefoldSolverEngine implements SolverEngine {

    /** Minimum interval between two progress events. */
    private static final long PROGRESS_INTERVAL_MILLIS = 1000L;

    private final TimefoldContributorRegistry contributors;
    private final ConstraintFamilyRegistry families;
    private final ObjectiveFamilyRegistry objectiveFamilies;
    private final PlanLimitsProperties limits;

    /**
     * @param contributors      the Timefold contributor registry
     * @param families          the constraint family registry (capability derivation)
     * @param objectiveFamilies the objective family registry (capability derivation)
     * @param limits            the safety limits ({@code timefold-unimproved-seconds})
     */
    @Autowired
    public TimefoldSolverEngine(TimefoldContributorRegistry contributors, ConstraintFamilyRegistry families,
                                ObjectiveFamilyRegistry objectiveFamilies, PlanLimitsProperties limits) {
        this.contributors = contributors;
        this.families = families;
        this.objectiveFamilies = objectiveFamilies;
        this.limits = limits == null ? new PlanLimitsProperties() : limits;
    }

    /** Convenience constructor with default limits (tests). */
    public TimefoldSolverEngine(TimefoldContributorRegistry contributors, ConstraintFamilyRegistry families,
                                ObjectiveFamilyRegistry objectiveFamilies) {
        this(contributors, families, objectiveFamilies, new PlanLimitsProperties());
    }

    @Override
    public String name() {
        return TimefoldCapabilities.ENGINE_ID;
    }

    @Override
    public EngineCapabilities capabilities() {
        return TimefoldCapabilities.of(contributors, families, objectiveFamilies);
    }

    @Override
    public SolveResult solve(CompiledProblem problem, SolveContext context) {
        final long startNanos = context.startNanos();
        final SolverOptions options = context.options() != null ? context.options() : problem.problem().options();
        final SolverCancellationToken token =
                context.cancellation() != null ? context.cancellation() : new SolverCancellationToken();
        final SolveListener listener = context.listener();
        final CandidateIndex index = problem.index();
        final int tier = tierOf(problem);
        final long maxSeconds = options.maxTimeSeconds();
        final long buildStartNanos = System.nanoTime();

        listener.onProgress(new SolveProgress(elapsedMillis(startNanos), 0, null, null, null, tier, "BUILD"));
        checkSupported(problem);

        SlotOptions frozen;
        TimefoldBuildContext buildContext;
        try {
            Runnable checkpoint = () -> SolveCancelledException.check(token, startNanos, maxSeconds);
            checkpoint.run();
            SlotOptions base = SlotOptionsBuilder.build(index, checkpoint);
            buildContext = new TimefoldBuildContext(problem, base);
            applyContributors(problem, buildContext, checkpoint);
            frozen = buildContext.freeze();
        } catch (SolveCancelledException e) {
            return unknown(e.getMessage(), problem, startNanos, buildStartNanos, 0, 0);
        }

        // A required task without any option cannot be scheduled: UNKNOWN here (INFEASIBLE is the pre-solve analyzer's job).
        List<TimefoldTask> entities = new ArrayList<>();
        List<String> emptyRequired = new ArrayList<>();
        for (int t = 0; t < index.taskCount(); t++) {
            if (frozen.taskOptionCount(t) > 0) {
                entities.add(new TimefoldTask(t, index.required(t), frozen));
            } else if (index.required(t)) {
                emptyRequired.add(index.taskId(t) + forbiddenBySuffix(buildContext, t));
            }
        }
        if (!emptyRequired.isEmpty()) {
            return unknown("No schedulable option remains for required task(s): " + String.join(", ", emptyRequired)
                    + " (infeasibility is not proven by this engine)", problem, startNanos, buildStartNanos,
                    frozen.optionCount(), entities.size());
        }
        long scoreBound = scoreUpperBound(frozen, index);
        long remainingMillis = context.remainingMillis(System.nanoTime());
        if (remainingMillis <= 0L) {
            return unknown(SolveCancelledException.TIME_LIMIT, problem, startNanos, buildStartNanos,
                    frozen.optionCount(), entities.size());
        }
        if (token.isCancellationRequested()) {
            return unknown(SolveCancelledException.STOP_REQUESTED, problem, startNanos, buildStartNanos,
                    frozen.optionCount(), entities.size());
        }
        if (entities.isEmpty()) {
            return finished(IEnumSolverStatus.Types.FEASIBLE, problem, frozen, List.of(), 0L, startNanos,
                    buildStartNanos, System.nanoTime(), tier, 0, null);
        }

        SolverConfig config = solverConfig(options, remainingMillis, scoreBound);
        // TIMEFOLD-VERIFY: SolverFactory.create(config).buildSolver() validates the domain model (entity-level
        // @ValueRangeProvider returning CountableValueRange<Integer>, allowsUnassigned) at the first solve.
        Solver<TimefoldPlan> solver = SolverFactory.<TimefoldPlan>create(config).buildSolver();

        final AtomicInteger solutions = new AtomicInteger();
        final AtomicLong firstSolutionMillis = new AtomicLong(-1L);
        final AtomicLong lastProgressMillis = new AtomicLong(Long.MIN_VALUE);
        solver.addEventListener(event -> {
            // TIMEFOLD-VERIFY: getNewBestScore() is the raw Score type; the cast matches the @PlanningScore type.
            if (!event.isNewBestSolutionInitialized() || !(event.getNewBestScore() instanceof HardSoftLongScore score)
                    || score.hardScore() < 0L) {
                return;
            }
            int count = solutions.incrementAndGet();
            long elapsed = elapsedMillis(startNanos);
            firstSolutionMillis.compareAndSet(-1L, elapsed);
            double objective = (double) -score.softScore();
            long last = lastProgressMillis.get();
            if (last == Long.MIN_VALUE || elapsed - last >= PROGRESS_INTERVAL_MILLIS) {
                lastProgressMillis.set(elapsed);
                listener.onProgress(new SolveProgress(elapsed, count, objective, null, null, tier, "SEARCH"));
            }
            if (listener.wantsIncumbents()) {
                listener.onIncumbent(new IncumbentSnapshot(elapsed,
                        extractAssignments(event.getNewBestSolution(), frozen, index), objective));
            }
        });

        long solveStartNanos = System.nanoTime();
        TimefoldPlan solved;
        try (SolverCancellationToken.Registration ignored = token.onCancellation(solver::terminateEarly)) {
            solved = solver.solve(new TimefoldPlan(frozen, entities));
        }
        long solveEndNanos = System.nanoTime();

        HardSoftLongScore score = solved.getScore();
        if (score == null) {
            return unknown("Timefold returned no score", problem, startNanos, buildStartNanos,
                    frozen.optionCount(), entities.size());
        }
        if (score.hardScore() < 0L) {
            // A heuristic stopped with violated hard constraints proves nothing: never INFEASIBLE.
            return new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(),
                    new Diagnostics("Timefold ended with hard score " + score.hardScore()
                            + " (hard violations remain; infeasibility is not proven)", List.of(), null, List.of(),
                            Map.of()),
                    metrics(problem, startNanos, buildStartNanos, solveStartNanos, solveEndNanos, frozen.optionCount(),
                            entities.size(), solutions.get(), firstSolutionMillis.get(), tier, null, null));
        }
        boolean stopped = token.isCancellationRequested() && Boolean.TRUE.equals(options.keepIncumbentOnStop());
        IEnumSolverStatus.Types status = stopped ? IEnumSolverStatus.Types.STOPPED_FEASIBLE
                : IEnumSolverStatus.Types.FEASIBLE;
        if (token.isCancellationRequested() && !stopped) {
            return unknown(SolveCancelledException.STOP_REQUESTED, problem, startNanos, buildStartNanos,
                    frozen.optionCount(), entities.size());
        }
        return finished(status, problem, frozen, solved.getTasks(), -score.softScore(), startNanos, buildStartNanos,
                solveEndNanos, tier, solutions.get(), firstSolutionMillis.get() < 0 ? null : firstSolutionMillis.get(),
                solveStartNanos);
    }

    // ------------------------------------------------------------------------------------------------ building

    private void applyContributors(CompiledProblem problem, TimefoldBuildContext ctx, Runnable checkpoint) {
        for (CompiledConstraint cc : problem.constraints()) {
            checkpoint.run();
            if (!cc.active()) {
                continue;
            }
            String type = cc.constraint().type();
            TimefoldConstraintContributor contributor = contributors.constraint(type).orElseThrow(() ->
                    new InvalidRequestException("ENGINE_UNSUPPORTED_CONSTRAINT",
                            "Engine 'timefold' does not support constraint type '" + type + "' (constraint '"
                                    + cc.constraint().id() + "')"));
            if (!contributor.support().supports(cc.constraint().enforcement())) {
                throw new InvalidRequestException("ENGINE_UNSUPPORTED_ENFORCEMENT",
                        "Engine 'timefold' does not support enforcement " + cc.constraint().enforcement()
                                + " for constraint '" + cc.constraint().id() + "' of type '" + type + "'");
            }
            contributor.contribute(ctx, cc);
        }
        for (CompiledObjective co : problem.objectives()) {
            checkpoint.run();
            if (co.objective().weight() == 0) {
                continue;
            }
            String type = co.objective().type();
            contributors.objective(type).orElseThrow(() -> new InvalidRequestException("ENGINE_UNSUPPORTED_OBJECTIVE",
                    "Engine 'timefold' does not support objective type '" + type + "' (objective '"
                            + co.objective().id() + "')")).contribute(ctx, co);
        }
    }

    /** Defensive re-check of the subset (the ProblemVerifier normally rejects these first); never silently ignored. */
    private void checkSupported(CompiledProblem problem) {
        CandidateIndex index = problem.index();
        if (index.edgeCount > 0) {
            unsupported("PRECEDENCES", "tasks have predecessors");
        }
        for (int r = 0; r < index.resourceCount(); r++) {
            if (index.resSetupMatrix[r] >= 0) {
                unsupported("SETUPS", "resource '" + index.resourceId(r) + "' has a setup matrix");
            }
            if (index.maxLoadTicks(r) < Integer.MAX_VALUE) {
                unsupported("MAX_LOAD_TICKS", "resource '" + index.resourceId(r) + "' has maxLoadTicks");
            }
        }
        if (!problem.problem().initialSolution().assignments().isEmpty()) {
            unsupported("INITIAL_SOLUTION", "an initial or locked solution was given");
        }
        for (var task : problem.problem().tasks()) {
            if (!task.resourceDurations().isEmpty()) {
                unsupported("RESOURCE_DURATIONS", "task '" + task.id() + "' has resourceDurations");
            }
        }
        Set<Integer> tiers = new TreeSet<>();
        for (CompiledObjective co : problem.objectives()) {
            if (co.objective().weight() > 0) {
                tiers.add(co.tier());
            }
            if (co.objective().weight() > 0 && "minimize_resource_cost".equals(co.objective().type())) {
                for (Resource resource : problem.problem().resources()) {
                    if (resource.costPerUse() != null && resource.costPerUse() > 0.0) {
                        unsupported("COST_PER_USE", "resource '" + resource.id() + "' has costPerUse");
                    }
                }
            }
        }
        for (CompiledConstraint cc : problem.constraints()) {
            if (cc.active() && cc.constraint().enforcement() == IEnumEnforcement.Types.SOFT) {
                tiers.add(cc.constraint().tier());
            }
        }
        if (tiers.size() > 1) {
            unsupported("LEXICOGRAPHIC", "more than one tier is used " + tiers);
        }
    }

    private static void unsupported(String feature, String detail) {
        throw new InvalidRequestException("ENGINE_UNSUPPORTED_FEATURE",
                "Engine 'timefold' does not support " + feature + ": " + detail);
    }

    private static int tierOf(CompiledProblem problem) {
        int tier = Integer.MAX_VALUE;
        for (CompiledObjective co : problem.objectives()) {
            if (co.objective().weight() > 0) {
                tier = Math.min(tier, co.tier());
            }
        }
        return tier == Integer.MAX_VALUE ? 0 : tier;
    }

    private static String forbiddenBySuffix(TimefoldBuildContext ctx, int task) {
        Set<String> ids = ctx.forbiddingConstraints(task);
        return ids.isEmpty() ? "" : " (options removed by " + String.join(", ", ids) + ")";
    }

    /**
     * Trivial upper bound of the soft score (capacity relaxed): a required task pays its cheapest option, an optional
     * task is selected only when its reward exceeds its cheapest option. Reaching it proves the plan best possible, so
     * it is used as the best-score termination limit. Guarded to 2^53 like every objective sum.
     */
    private static long scoreUpperBound(SlotOptions options, CandidateIndex index) {
        long bound = 0L;
        long magnitude = 0L;
        for (int t = 0; t < options.taskCount(); t++) {
            if (options.taskOptionCount(t) == 0) {
                continue;
            }
            long minPenalty = Long.MAX_VALUE;
            long maxPenalty = 0L;
            for (int o = options.taskOptionStart(t); o < options.taskOptionEnd(t); o++) {
                minPenalty = Math.min(minPenalty, options.penalty(o));
                maxPenalty = Math.max(maxPenalty, options.penalty(o));
            }
            long reward = options.reward(t);
            long best = index.required(t) ? -minPenalty : Math.max(0L, reward - minPenalty);
            bound = ScaledMath.guard(ScaledMath.addExact(bound, best), "Timefold score bound");
            magnitude = ScaledMath.guard(ScaledMath.addExact(magnitude, ScaledMath.addExact(maxPenalty, reward)),
                    "Timefold score magnitude");
        }
        return bound;
    }

    private SolverConfig solverConfig(SolverOptions options, long remainingMillis, long scoreBound) {
        TerminationConfig termination = new TerminationConfig()
                .withSpentLimit(Duration.ofMillis(remainingMillis))
                .withBestScoreLimit(HardSoftLongScore.of(0L, scoreBound).toString());
        // TIMEFOLD-VERIFY: bestScoreLimit takes the score string of the @PlanningScore type ("0hard/-5soft").
        long unimprovedSeconds = limits.getTimefoldUnimprovedSeconds();
        if (unimprovedSeconds > 0 && unimprovedSeconds * 1000L < remainingMillis) {
            termination.setUnimprovedSpentLimit(Duration.ofSeconds(unimprovedSeconds));
        }
        boolean deterministic = Boolean.TRUE.equals(options.deterministic());
        return new SolverConfig()
                .withSolutionClass(TimefoldPlan.class)
                .withEntityClasses(TimefoldTask.class)
                .withConstraintProviderClass(PlanConstraintProvider.class)
                .withTerminationConfig(termination)
                .withMoveThreadCount("NONE")
                .withEnvironmentMode(deterministic ? EnvironmentMode.REPRODUCIBLE : EnvironmentMode.NON_REPRODUCIBLE)
                .withRandomSeed(options.seed());
    }

    // ------------------------------------------------------------------------------------------------ results

    private SolveResult finished(IEnumSolverStatus.Types status, CompiledProblem problem, SlotOptions frozen,
                                 List<TimefoldTask> tasks, long tierValue, long startNanos, long buildStartNanos,
                                 long endNanos, int tier, int solutions, Long firstSolutionMillis) {
        return finished(status, problem, frozen, tasks, tierValue, startNanos, buildStartNanos, endNanos, tier,
                solutions, firstSolutionMillis, endNanos);
    }

    private SolveResult finished(IEnumSolverStatus.Types status, CompiledProblem problem, SlotOptions frozen,
                                 List<TimefoldTask> tasks, long tierValue, long startNanos, long buildStartNanos,
                                 long endNanos, int tier, int solutions, Long firstSolutionMillis,
                                 long solveStartNanos) {
        CandidateIndex index = problem.index();
        List<Assignment> assignments = new ArrayList<>();
        boolean[] selected = new boolean[index.taskCount()];
        for (TimefoldTask task : tasks) {
            if (task.getOption() != null) {
                assignments.add(assignment(index, frozen, task.getTaskIdx(), task.getOption()));
                selected[task.getTaskIdx()] = true;
            }
        }
        assignments.sort(Comparator.comparingInt(Assignment::startTick).thenComparing(Assignment::taskId));
        List<UnassignedTask> unassigned = new ArrayList<>();
        for (int t = 0; t < index.taskCount(); t++) {
            if (!selected[t] && !index.required(t)) {
                unassigned.add(new UnassignedTask(index.taskId(t), "NOT_SELECTED",
                        "Optional task was not selected by the solver", null, index.prize(t)));
            }
        }
        unassigned.sort(Comparator.comparing(UnassignedTask::taskId));
        PlanningSolution solution = new PlanningSolution(assignments, unassigned, List.of(), List.of(), List.of(), 0.5);
        Diagnostics diagnostics = new Diagnostics("Timefold is a heuristic engine: the plan is feasible, optimality is not proven",
                List.of(), null, List.of(), Map.of());
        return new SolveResult(status, solution, List.of(), diagnostics,
                metrics(problem, startNanos, buildStartNanos, solveStartNanos, endNanos, frozen.optionCount(),
                        tasks.size(), solutions, firstSolutionMillis == null ? -1L : firstSolutionMillis, tier,
                        tierValue, (double) tierValue));
    }

    private static List<Assignment> extractAssignments(TimefoldPlan plan, SlotOptions frozen, CandidateIndex index) {
        List<Assignment> assignments = new ArrayList<>();
        for (TimefoldTask task : plan.getTasks()) {
            if (task.getOption() != null) {
                assignments.add(assignment(index, frozen, task.getTaskIdx(), task.getOption()));
            }
        }
        assignments.sort(Comparator.comparingInt(Assignment::startTick).thenComparing(Assignment::taskId));
        return assignments;
    }

    private static Assignment assignment(CandidateIndex index, SlotOptions options, int task, int option) {
        int start = options.start(option);
        int end = options.end(option);
        int candidate = options.candidate(option);
        String role = index.roles[index.reqRole[index.taskReqStart[task]]];
        return new Assignment(index.taskId(task), !index.required(task), start, end,
                index.horizon().toInstant(start), index.horizon().toInstant(end), index.slotId(options.slot(option)),
                List.of(new ResourceAssignment(index.resourceId(options.resource(option)), role,
                        index.candidateAmount(candidate))));
    }

    private SolveResult unknown(String message, CompiledProblem problem, long startNanos, long buildStartNanos,
                                int options, int entities) {
        long now = System.nanoTime();
        return new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(),
                new Diagnostics(message, List.of(), null, List.of(), Map.of()),
                metrics(problem, startNanos, buildStartNanos, now, now, options, entities, 0, -1L, tierOf(problem),
                        null, null));
    }

    private static Metrics metrics(CompiledProblem problem, long startNanos, long buildStartNanos, long solveStartNanos,
                                   long endNanos, int optionCount, int entities, int solutions, long firstSolutionMillis,
                                   int tier, Long tierValue, Double objective) {
        CandidateIndex index = problem.index();
        long buildMillis = Math.max(0L, (solveStartNanos - buildStartNanos) / 1_000_000L);
        long solveMillis = Math.max(0L, (endNanos - solveStartNanos) / 1_000_000L);
        long durationMillis = Math.max(0L, (endNanos - startNanos) / 1_000_000L);
        ModelStats model = new ModelStats(index.taskCount(), index.resourceCount(), optionCount, 0, entities, 4, 0, 0, 0,
                0, 0);
        SearchStats search = new SearchStats(0L, 0L, solutions, objective, null, null, tierValue == null ? 0 : 1);
        List<TierValue> tiers = tierValue == null ? List.of() : List.of(new TierValue(tier, tierValue, null, "FEASIBLE"));
        return new Metrics(durationMillis, buildMillis, solveMillis, firstSolutionMillis < 0 ? null : firstSolutionMillis,
                1, model, search, tiers);
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }
}

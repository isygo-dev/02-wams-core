package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.RelaxationPolicy;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.cpsat.explain.CpSatExplainer;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.index.CandidateIndexBuilder;
import eu.isygoit.solver.registry.CpSatContributorRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test harness of the WP3 solver tests: tiny hand-written contributors (so the tests do not depend on the family batches),
 * problem builders over the real shared records, and the real {@code CandidateIndexBuilder}. Everything that touches
 * OR-Tools is only reached after {@link #nativesAvailable()} returned true.
 */
final class CpSatTestKit {

    static final Instant START = Instant.parse("2026-09-07T08:00:00Z");

    private CpSatTestKit() {
    }

    // ------------------------------------------------------------------------------------------------ natives

    static boolean nativesAvailable() {
        return newEngine(List.of(), List.of(), new CpSatProperties()).isAvailable();
    }

    // ------------------------------------------------------------------------------------------------ engine

    static CpSatSolverEngine newEngine(List<CpSatConstraintContributor> constraints,
                                       List<CpSatObjectiveContributor> objectives, CpSatProperties properties) {
        CpModelBuilder builder = newBuilder(constraints, objectives, properties);
        return new CpSatSolverEngine(builder, new CpSatExplainer(), properties);
    }

    static CpModelBuilder newBuilder(List<CpSatConstraintContributor> constraints,
                                     List<CpSatObjectiveContributor> objectives, CpSatProperties properties) {
        return new CpModelBuilder(new CpSatContributorRegistry(constraints, objectives),
                List.of(new SymmetryBreakingModule(properties)), properties);
    }

    static List<CpSatObjectiveContributor> objectives() {
        return List.of(new MakespanContributor(), new ValueContributor());
    }

    static List<CpSatConstraintContributor> constraints() {
        return List.of(new LatestEndContributor());
    }

    // ------------------------------------------------------------------------------------------------ problem

    static Horizon horizon(int ticks) {
        return new Horizon(START, START.plusSeconds(60L * ticks), 60);
    }

    static Instant at(int tick) {
        return START.plusSeconds(60L * tick);
    }

    static SolverOptions options(int maxSeconds, int workers, boolean deterministic) {
        return new SolverOptions(null, maxSeconds, workers, 0L, false, null, deterministic, null, null, null, null, null,
                null);
    }

    static Resource resource(String id, int capacity) {
        return new Resource(id, null, null, capacity, null, null, null, null, null, null, null, null, null);
    }

    static Resource resourceWithCalendar(String id, String calendarId) {
        return new Resource(id, null, null, 1, null, null, calendarId, null, null, null, null, null, null);
    }

    static Calendar calendar(String id, int fromTick, int toTick) {
        return new Calendar(id, id, null,
                List.of(new eu.isygoit.dto.request.TimeWindow(at(fromTick), at(toTick))), null, null, null);
    }

    static ResourceRequirement on(String... resourceIds) {
        return new ResourceRequirement("machine",
                new ResourceSelector(List.of(resourceIds), null, null, null, null, null), 1, null);
    }

    static ResourceRequirement any(int count) {
        return new ResourceRequirement("worker", ResourceSelector.all(), count, null);
    }

    static Task task(String id, int duration) {
        return task(id, duration, true, null, List.of(), List.of());
    }

    static Task task(String id, int duration, boolean required, Long prize, List<ResourceRequirement> requirements,
                     List<PrecedenceRef> predecessors) {
        return new Task(id, null, null, duration, null, null, null, required, prize, requirements, null, null, null, null,
                null, predecessors, null, null, null, null);
    }

    static Task slotTask(String id, int duration, List<String> slotIds) {
        return new Task(id, null, null, duration, null, null, null, true, null, List.of(), null, null, null, slotIds,
                null, List.of(), null, null, null, null);
    }

    static Slot slot(String id, int fromTick, int toTick) {
        return new Slot(id, null, id, at(fromTick), at(toTick), 0.0d, null);
    }

    static PlanningProblem problem(int horizonTicks, List<Resource> resources, List<Task> tasks, SolverOptions options) {
        return problem(horizonTicks, List.of(), List.of(), resources, tasks, options, null);
    }

    static PlanningProblem problem(int horizonTicks, List<Slot> slots, List<Calendar> calendars,
                                   List<Resource> resources, List<Task> tasks, SolverOptions options,
                                   InitialSolution initial) {
        return new PlanningProblem(1L, "test", "UTC", horizon(horizonTicks), slots, calendars, List.of(), resources,
                tasks, List.of(), List.of(), options, initial);
    }

    static CompiledProblem compile(PlanningProblem problem, List<CompiledConstraint> constraints,
                                   List<CompiledObjective> objectives) {
        CandidateIndex index = CandidateIndexBuilder.build(problem, constraints, new PlanLimitsProperties(),
                new SolverCancellationToken());
        return new CompiledProblem(problem, index, null, constraints, objectives);
    }

    static CompiledObjective objective(int index, String type, int weight, int tier) {
        return new CompiledObjective(index, new Objective("o" + index, type, weight, tier, null, null, Map.of()), null,
                null, tier, 100);
    }

    static CompiledConstraint latestEnd(int index, String id, String taskId, int latestEnd,
                                        IEnumEnforcement.Types enforcement, int priority) {
        RelaxationPolicy policy = enforcement == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                ? new RelaxationPolicy(true, priority, 1.0d) : null;
        Constraint constraint = new Constraint(id, LatestEndContributor.TYPE, enforcement,
                new ConstraintScope("task", taskId), Map.of("latestEnd", latestEnd), policy, 1, 0);
        return new CompiledConstraint(index, constraint, null, null, true);
    }

    static SolveContext context(SolverOptions options, SolverCancellationToken token) {
        return new SolveContext(options, token == null ? new SolverCancellationToken() : token, SolveListener.NOOP,
                System.nanoTime());
    }

    static SolveResult solve(CpSatSolverEngine engine, CompiledProblem problem) {
        return engine.solve(problem, context(problem.problem().options(), null));
    }

    // ------------------------------------------------------------------------------------------------ results

    static Assignment assignment(SolveResult result, String taskId) {
        return result.solution().assignments().stream().filter(a -> a.taskId().equals(taskId)).findFirst()
                .orElseThrow(() -> new AssertionError("task not assigned: " + taskId));
    }

    static TierValue tier(SolveResult result, int tier) {
        return result.metrics().tiers().stream().filter(t -> t.tier() == tier).findFirst()
                .orElseThrow(() -> new AssertionError("no tier value for tier " + tier));
    }

    static List<String> ids(List<Assignment> assignments) {
        List<String> ids = new ArrayList<>();
        for (Assignment assignment : assignments) {
            ids.add(assignment.taskId());
        }
        return ids;
    }

    // ------------------------------------------------------------------------------------- test contributors

    /** {@code test_makespan}: minimises the maximum end (weight x makespan) in the objective's tier. */
    static final class MakespanContributor implements CpSatObjectiveContributor {
        static final String TYPE = "test_makespan";

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
            long horizon = ctx.index().horizonTicks();
            IntVar makespan = ctx.newTierVar("makespan", 0, horizon);
            for (int t = 0; t < ctx.index().taskCount(); t++) {
                CpModelContext.enforce(ctx.model().addGreaterOrEqual(makespan, ctx.end(t)),
                        ctx.presence(t) == ctx.trueLit() ? new Literal[0] : new Literal[]{ctx.presence(t)});
            }
            int weight = objective.objective().weight();
            sink.add(objective.tier(), LinearExpr.term(makespan, weight), 0L, horizon * weight, "makespan");
        }
    }

    /** {@code test_value}: maximises the prize of the selected optional tasks (negated term). */
    static final class ValueContributor implements CpSatObjectiveContributor {
        static final String TYPE = "test_value";

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
            LinearExprBuilder terms = LinearExpr.newBuilder();
            long total = 0;
            for (int t = 0; t < ctx.index().taskCount(); t++) {
                if (!ctx.index().required(t)) {
                    terms.addTerm(ctx.presence(t), -ctx.index().prize(t));
                    total += ctx.index().prize(t);
                }
            }
            sink.add(objective.tier(), terms.build(), -total, 0L, "value");
        }
    }

    /**
     * {@code test_latest_end}: the scoped task ends at or before {@code params.latestEnd} (HARD or RELAXABLE through the
     * enforcer literal; SOFT penalises the late task by one unit).
     */
    static final class LatestEndContributor implements CpSatConstraintContributor {
        static final String TYPE = "test_latest_end";

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public EnforcementSupport support() {
            return EnforcementSupport.ALL;
        }

        @Override
        public void apply(CpModelContext ctx, CompiledConstraint compiled, Enforcer enforcer) {
            int bound = ((Number) compiled.constraint().params().get("latestEnd")).intValue();
            for (int t : ctx.index().resolveTasks(compiled.constraint().scope())) {
                Literal presence = ctx.presence(t) == ctx.trueLit() ? null : ctx.presence(t);
                CpModelContext.enforce(ctx.model().addLessOrEqual(ctx.end(t), bound),
                        enforcer.conditions(enforcer.instance(t, -1), presence));
            }
        }
    }
}

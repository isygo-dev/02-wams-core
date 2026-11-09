package eu.isygoit.solver.timefold;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.index.CandidateIndexBuilder;
import eu.isygoit.solver.registry.TimefoldContributorRegistry;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.timefold.contrib.FixedAssignmentTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.MaximizeSelectedValueTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.MaximizeTaskPriorityTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.MinimizeResourceCostTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.MinimizeSlotCostTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.ResourceAvailabilityTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.ResourceCapacityTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.TaskEligibilityTimefoldContributor;
import eu.isygoit.solver.timefold.contrib.TimeWindowTimefoldContributor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hand-built compiled problems for the Timefold tests (the golden JSON files and the real PlanningService belong to the
 * integration tests of WP8b). Problems are built directly from the IR records and indexed with the real
 * {@link CandidateIndexBuilder}; objective families are Mockito stubs because neither the index builder (no constraints
 * are passed) nor the Timefold engine calls them. All durations are explicit (the normalizer is bypassed).
 */
final class TimefoldTestProblems {

    static final Instant T0 = Instant.parse("2026-09-07T08:00:00Z");

    private TimefoldTestProblems() {
    }

    /** All real Timefold contributors, registered like the Spring wiring does. */
    static TimefoldContributorRegistry contributorRegistry() {
        return new TimefoldContributorRegistry(
                List.of(new TaskEligibilityTimefoldContributor(), new ResourceCapacityTimefoldContributor(),
                        new TimeWindowTimefoldContributor(), new ResourceAvailabilityTimefoldContributor(),
                        new FixedAssignmentTimefoldContributor()),
                List.of(new MinimizeSlotCostTimefoldContributor(), new MinimizeResourceCostTimefoldContributor(),
                        new MaximizeTaskPriorityTimefoldContributor(), new MaximizeSelectedValueTimefoldContributor()));
    }

    /** An engine with the default limits except a short unimproved limit (fast tests). */
    static TimefoldSolverEngine engine() {
        PlanLimitsProperties limits = new PlanLimitsProperties();
        limits.setTimefoldUnimprovedSeconds(1);
        return new TimefoldSolverEngine(contributorRegistry(), null, null, limits);
    }

    static SolverOptions options(boolean deterministic) {
        return new SolverOptions("timefold", 5, 1, 7L, false, null, deterministic, null, null, null, null, null, null);
    }

    static SolveContext context(SolverOptions options, SolverCancellationToken token, SolveListener listener) {
        return new SolveContext(options, token, listener, System.nanoTime());
    }

    static Resource resource(String id, int capacity, Set<String> skills, Double costPerTick, List<TimeWindow> unavailable) {
        return new Resource(id, "resource", id, capacity, skills, null, null, unavailable, costPerTick, null, null, null,
                null);
    }

    static Slot slot(String id, Instant start, Instant end, double cost) {
        return new Slot(id, "period", id, start, end, cost, null);
    }

    /** A task with one implicit-style requirement selecting resources with ALL the given skills. */
    static Task task(String id, int duration, boolean required, int priority, List<String> skills, List<String> slots) {
        ResourceRequirement requirement = new ResourceRequirement("default",
                new ResourceSelector(null, null, skills, null, null, null), 1, null);
        return new Task(id, null, id, duration, null, 1, priority, required, null, List.of(requirement), null, null, null,
                slots, null, null, null, null, null, null);
    }

    static PlanningProblem problem(Horizon horizon, List<Slot> slots, List<Resource> resources, List<Task> tasks,
                                   List<Objective> objectives, SolverOptions options) {
        return new PlanningProblem(1L, "test", "UTC", horizon, slots, null, null, resources, tasks, null, objectives,
                options, null);
    }

    /** Indexes the problem (no constraints) and wraps the objectives with stub families. */
    static CompiledProblem compile(PlanningProblem problem) {
        CandidateIndex index = CandidateIndexBuilder.build(problem, List.of(), new PlanLimitsProperties(),
                new SolverCancellationToken());
        List<CompiledObjective> objectives = new ArrayList<>();
        int i = 0;
        for (Objective objective : problem.objectives()) {
            objectives.add(new CompiledObjective(i++, objective, Mockito.mock(ObjectiveFamily.class), null,
                    objective.tier(), problem.options().objectiveScale()));
        }
        return new CompiledProblem(problem, index, ModelCost.ZERO, List.of(), objectives);
    }

    // ------------------------------------------------------------------------------------------------ scenarios

    /** G1: three one-hour sessions, one lab of capacity 2, slots early (cost 0) and late (cost 1.0), minimize_slot_cost. */
    static CompiledProblem g1(boolean deterministic) {
        Horizon horizon = new Horizon(T0, T0.plusSeconds(4 * 3600), 900);
        List<Slot> slots = List.of(
                slot("slot-early", T0, T0.plusSeconds(3600), 0.0),
                slot("slot-late", T0.plusSeconds(3600), T0.plusSeconds(7200), 1.0));
        List<Resource> resources = List.of(resource("lab", 2, Set.of(), null, null));
        List<String> both = List.of("slot-early", "slot-late");
        List<Task> tasks = List.of(
                task("session-1", 4, true, 0, null, both),
                task("session-2", 4, true, 0, null, both),
                task("session-3", 4, true, 0, null, both));
        return compile(problem(horizon, slots, resources, tasks,
                List.of(new Objective("slot-cost", "minimize_slot_cost", 1)), options(deterministic)));
    }

    /** G3: one required + three optional tasks (priority 9/5/1), one lab of capacity 1, two slots, maximize_task_priority. */
    static CompiledProblem g3(boolean deterministic) {
        Horizon horizon = new Horizon(T0, T0.plusSeconds(4 * 3600), 900);
        List<Slot> slots = List.of(
                slot("slot-1", T0, T0.plusSeconds(3600), 0.0),
                slot("slot-2", T0.plusSeconds(3600), T0.plusSeconds(7200), 0.0));
        List<Resource> resources = List.of(resource("lab", 1, Set.of(), null, null));
        List<String> both = List.of("slot-1", "slot-2");
        List<Task> tasks = List.of(
                task("mandatory-session", 4, true, 0, null, both),
                task("optional-high", 4, false, 9, null, both),
                task("optional-medium", 4, false, 5, null, both),
                task("optional-low", 4, false, 1, null, both));
        return compile(problem(horizon, slots, resources, tasks,
                List.of(new Objective("priority", "maximize_task_priority", 1)), options(deterministic)));
    }

    /** G12 (without the redundant class no_overlap, which the Timefold subset rejects): the school timetable of 2.6 (a). */
    static CompiledProblem g12(boolean deterministic) {
        Instant start = Instant.parse("2026-09-07T06:00:00Z");
        Horizon horizon = new Horizon(start, Instant.parse("2026-09-08T08:15:00Z"), 900);
        List<Slot> slots = List.of(
                slot("mon-p1", start, Instant.parse("2026-09-07T07:00:00Z"), 0.0),
                slot("mon-p2", Instant.parse("2026-09-07T07:15:00Z"), Instant.parse("2026-09-07T08:15:00Z"), 0.0),
                slot("tue-p1", Instant.parse("2026-09-08T06:00:00Z"), Instant.parse("2026-09-08T07:00:00Z"), 1.0),
                slot("tue-p2", Instant.parse("2026-09-08T07:15:00Z"), Instant.parse("2026-09-08T08:15:00Z"), 1.0));
        List<Resource> resources = List.of(
                resource("teacher-alice", 1, Set.of("math"), null, null),
                resource("teacher-bob", 1, Set.of("science"), null, null));
        List<String> p1 = List.of("mon-p1", "tue-p1");
        List<String> p2 = List.of("mon-p2", "tue-p2");
        List<Task> tasks = List.of(
                task("7a-math", 4, true, 0, List.of("math"), p1),
                task("7b-math", 4, true, 0, List.of("math"), p1),
                task("7a-science", 4, true, 0, List.of("science"), p2),
                task("7b-science", 4, true, 0, List.of("science"), p2));
        return compile(problem(horizon, slots, resources, tasks,
                List.of(new Objective("slot-cost", "minimize_slot_cost", 1)), options(deterministic)));
    }
}

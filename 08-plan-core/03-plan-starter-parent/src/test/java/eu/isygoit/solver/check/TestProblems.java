package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Pin;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumSolverStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test fixtures for the WP6 tests: tiny, readable builders over the wide request/response records. Tick 0 is
 * 2026-01-01T00:00Z and one tick is one minute.
 */
public final class TestProblems {

    public static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private TestProblems() {
    }

    public static Horizon horizon(int ticks) {
        return new Horizon(T0, T0.plusSeconds(ticks * 60L), 60);
    }

    public static Instant at(int tick) {
        return T0.plusSeconds(tick * 60L);
    }

    public static Resource resource(String id, int capacity, String... skills) {
        return new Resource(id, null, id, capacity, Set.of(skills), Set.of(), null, List.of(), null, null,
                null, null, Map.of());
    }

    public static Resource resourceWith(String id, int capacity, List<TimeWindow> unavailable,
                                        Integer maxLoadTicks, String setupMatrixId) {
        return new Resource(id, null, id, capacity, Set.of(), Set.of(), null, unavailable, null, null,
                maxLoadTicks, setupMatrixId, Map.of());
    }

    public static ResourceRequirement requirement(String role, int count, Integer amount, String... skills) {
        return new ResourceRequirement(role, new ResourceSelector(null, null, List.of(skills), null, null, null),
                count, amount);
    }

    public static ResourceRequirement requirementOn(String role, String... resourceIds) {
        return new ResourceRequirement(role, new ResourceSelector(List.of(resourceIds), null, null, null, null, null),
                1, null);
    }

    /** Fluent task builder. */
    public static final class TaskB {
        private final String id;
        private Integer duration;
        private int demand = 1;
        private boolean required = true;
        private Long prize;
        private List<ResourceRequirement> requirements = List.of();
        private Instant release;
        private Instant deadline;
        private List<String> startSlots = List.of();
        private Pin pin;
        private final List<PrecedenceRef> predecessors = new ArrayList<>();
        private String setupClass;
        private Map<String, Integer> resourceDurations = Map.of();

        TaskB(String id, Integer duration) {
            this.id = id;
            this.duration = duration;
        }

        public TaskB demand(int v) {
            this.demand = v;
            return this;
        }

        public TaskB optional(Long prize) {
            this.required = false;
            this.prize = prize;
            return this;
        }

        public TaskB requires(ResourceRequirement... reqs) {
            this.requirements = List.of(reqs);
            return this;
        }

        public TaskB release(int tick) {
            this.release = at(tick);
            return this;
        }

        public TaskB deadline(int tick) {
            this.deadline = at(tick);
            return this;
        }

        public TaskB slots(String... ids) {
            this.startSlots = List.of(ids);
            return this;
        }

        public TaskB pin(Integer startTick, String... resourceIds) {
            this.pin = new Pin(startTick == null ? null : at(startTick), List.of(resourceIds));
            return this;
        }

        public TaskB after(String predecessor, PrecedenceRef.Type type, int minLag, Integer maxLag,
                           PrecedenceRef.Presence presence) {
            this.predecessors.add(new PrecedenceRef(predecessor, type, minLag, maxLag, presence));
            return this;
        }

        public TaskB after(String predecessor) {
            return after(predecessor, PrecedenceRef.Type.FINISH_START, 0, null, PrecedenceRef.Presence.WHEN_BOTH);
        }

        public TaskB setupClass(String c) {
            this.setupClass = c;
            return this;
        }

        public TaskB resourceDurations(Map<String, Integer> m) {
            this.resourceDurations = m;
            return this;
        }

        public Task build() {
            return new Task(id, null, id, duration, resourceDurations, demand, 0, required, prize, requirements,
                    release, deadline, null, startSlots, pin, predecessors, setupClass, Set.of(), List.of(), Map.of());
        }
    }

    public static TaskB task(String id, int duration) {
        return new TaskB(id, duration);
    }

    public static Slot slot(String id, int startTick, int endTick) {
        return new Slot(id, null, id, at(startTick), at(endTick), 0.0, Map.of());
    }

    public static PlanningProblem problem(int horizonTicks, List<Resource> resources, List<Task> tasks,
                                          List<Constraint> constraints, List<Objective> objectives) {
        return new PlanningProblem(1L, "test", "UTC", horizon(horizonTicks), List.of(), List.of(), List.of(),
                resources, tasks, constraints, objectives, SolverOptions.defaults(), new InitialSolution(null));
    }

    public static PlanningProblem problem(int horizonTicks, List<Slot> slots, List<SetupMatrix> matrices,
                                          List<Resource> resources, List<Task> tasks, InitialSolution initial) {
        return new PlanningProblem(1L, "test", "UTC", horizon(horizonTicks), slots, List.of(), matrices,
                resources, tasks, List.of(), List.of(), SolverOptions.defaults(), initial);
    }

    // ------------------------------------------------------------------------------------------ result builders

    public static Assignment assign(String taskId, int start, int end, String... resourceIds) {
        return assign(taskId, start, end, 1, false, resourceIds);
    }

    public static Assignment assign(String taskId, int start, int end, int amount, boolean optional,
                                    String... resourceIds) {
        List<ResourceAssignment> resources = new ArrayList<>();
        for (String id : resourceIds) {
            resources.add(new ResourceAssignment(id, "default", amount));
        }
        return new Assignment(taskId, optional, start, end, at(start), at(end), null, resources);
    }

    public static SolveResult result(IEnumSolverStatus.Types status, Assignment... assignments) {
        PlanningSolution solution = new PlanningSolution(List.of(assignments), List.of(), List.of(), List.of(),
                List.of(), 1.0);
        return new SolveResult(status, solution, List.of(), null, null);
    }
}

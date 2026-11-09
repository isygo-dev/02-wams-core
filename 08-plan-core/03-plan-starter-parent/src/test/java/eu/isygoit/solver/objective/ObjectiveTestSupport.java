package eu.isygoit.solver.objective;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Preference;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.util.ScaledMath;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hand-built fakes shared by the objective family tests: problem builders, an in-memory {@link CandidateIndexView}, a
 * {@link SolutionView} and a {@link CheckContext}. Tick 0 = {@link #START}; tick length 60 s; horizon 480 ticks.
 */
final class ObjectiveTestSupport {

    static final Instant START = Instant.parse("2026-09-07T08:00:00Z");
    static final int HORIZON_TICKS = 480;

    private ObjectiveTestSupport() {
    }

    static Horizon horizon() {
        return new Horizon(START, START.plusSeconds(HORIZON_TICKS * 60L), 60);
    }

    static Instant at(int tick) {
        return START.plusSeconds(tick * 60L);
    }

    // ------------------------------------------------------------------ builders

    static TaskBuilder task(String id) {
        return new TaskBuilder(id);
    }

    /** Mutable builder for the 20-component {@link Task} record. */
    static final class TaskBuilder {
        private final String id;
        private Integer duration = 10;
        private Integer priority;
        private Boolean required;
        private Long prize;
        private Instant dueDate;
        private String setupClass;
        private Set<String> tags = new LinkedHashSet<>();
        private List<Preference> preferences = new ArrayList<>();

        private TaskBuilder(String id) {
            this.id = id;
        }

        TaskBuilder duration(int ticks) {
            this.duration = ticks;
            return this;
        }

        TaskBuilder priority(int priority) {
            this.priority = priority;
            return this;
        }

        TaskBuilder optional() {
            this.required = Boolean.FALSE;
            return this;
        }

        TaskBuilder prize(long prize) {
            this.prize = prize;
            return this;
        }

        TaskBuilder dueTick(int tick) {
            this.dueDate = at(tick);
            return this;
        }

        TaskBuilder setupClass(String setupClass) {
            this.setupClass = setupClass;
            return this;
        }

        TaskBuilder tag(String tag) {
            this.tags.add(tag);
            return this;
        }

        TaskBuilder preference(Preference preference) {
            this.preferences.add(preference);
            return this;
        }

        Task build() {
            return new Task(id, null, null, duration, null, null, priority, required, prize, null, null, null,
                    dueDate, null, null, null, setupClass, tags, preferences, null);
        }
    }

    static Resource resource(String id) {
        return resource(id, 1, 0.0, 0.0, null);
    }

    static Resource resource(String id, int capacity, double costPerTick, double costPerUse, String setupMatrixId) {
        return new Resource(id, null, null, capacity, null, null, null, null, costPerTick, costPerUse, null,
                setupMatrixId, null);
    }

    static Slot slot(String id, int startTick, int endTick, double cost) {
        return new Slot(id, null, null, at(startTick), at(endTick), cost, null);
    }

    static PlanningProblem problem(List<Task> tasks, List<Resource> resources) {
        return problem(tasks, resources, List.of(), List.of(), new InitialSolution(List.of()), List.of());
    }

    static PlanningProblem problem(List<Task> tasks, List<Resource> resources, List<Slot> slots,
                                   List<SetupMatrix> matrices, InitialSolution initial, List<Objective> objectives) {
        return new PlanningProblem(1L, "test", "UTC", horizon(), slots, List.of(), matrices, resources, tasks,
                List.<Constraint>of(), objectives, SolverOptions.defaults(), initial);
    }

    static InitialSolution initial(InitialAssignment... assignments) {
        return new InitialSolution(Arrays.asList(assignments));
    }

    static Objective objective(String type, int weight) {
        return new Objective("obj", type, weight);
    }

    static Objective objective(String type, ConstraintScope scope, int scale) {
        return new Objective("obj", type, 1, 0, scale, scope, java.util.Map.of());
    }

    // ------------------------------------------------------------------ fakes

    /** In-memory index: one candidate per (task, listed resource), duration = task duration. */
    static final class FakeIndex implements CandidateIndexView {
        private final PlanningProblem problem;
        private final int[] candBegin;
        private final int[] candTask;
        private final int[] candResource;
        private final int[] candDuration;

        /**
         * @param problem            the problem
         * @param candidateResources resource indices usable by each task (the candidates of the task)
         */
        FakeIndex(PlanningProblem problem, int[][] candidateResources) {
            this.problem = problem;
            int n = problem.tasks().size();
            this.candBegin = new int[n + 1];
            List<int[]> rows = new ArrayList<>();
            for (int t = 0; t < n; t++) {
                candBegin[t] = rows.size();
                int d = duration(t);
                for (int r : candidateResources[t]) {
                    rows.add(new int[]{t, r, d});
                }
            }
            candBegin[n] = rows.size();
            candTask = new int[rows.size()];
            candResource = new int[rows.size()];
            candDuration = new int[rows.size()];
            for (int c = 0; c < rows.size(); c++) {
                candTask[c] = rows.get(c)[0];
                candResource[c] = rows.get(c)[1];
                candDuration[c] = rows.get(c)[2];
            }
        }

        @Override public int taskCount() { return problem.tasks().size(); }
        @Override public int resourceCount() { return problem.resources().size(); }
        @Override public int slotCount() { return problem.slots().size(); }
        @Override public Horizon horizon() { return problem.horizon(); }
        @Override public int horizonTicks() { return (int) problem.horizon().horizonTicks(); }
        @Override public PlanLimitsProperties limits() { return new PlanLimitsProperties(); }
        @Override public String taskId(int t) { return problem.tasks().get(t).id(); }
        @Override public String resourceId(int r) { return problem.resources().get(r).id(); }
        @Override public String slotId(int s) { return problem.slots().get(s).id(); }

        @Override
        public int taskIdx(String id) {
            for (int t = 0; t < taskCount(); t++) {
                if (taskId(t).equals(id)) {
                    return t;
                }
            }
            return -1;
        }

        @Override
        public int resourceIdx(String id) {
            for (int r = 0; r < resourceCount(); r++) {
                if (resourceId(r).equals(id)) {
                    return r;
                }
            }
            return -1;
        }

        @Override
        public int slotIdx(String id) {
            for (int s = 0; s < slotCount(); s++) {
                if (slotId(s).equals(id)) {
                    return s;
                }
            }
            return -1;
        }

        @Override
        public int[] resolveTasks(ConstraintScope scope) {
            List<Integer> out = new ArrayList<>();
            for (int t = 0; t < taskCount(); t++) {
                Task task = problem.tasks().get(t);
                boolean in = switch (scope.kind()) {
                    case "all" -> true;
                    case "task" -> task.id().equals(scope.value());
                    case "tag" -> task.tags().contains(scope.value());
                    default -> false;
                };
                if (in) {
                    out.add(t);
                }
            }
            return out.stream().mapToInt(Integer::intValue).toArray();
        }

        @Override
        public int[] resolveResources(ConstraintScope scope) {
            List<Integer> out = new ArrayList<>();
            for (int r = 0; r < resourceCount(); r++) {
                boolean in = switch (scope.kind()) {
                    case "all" -> true;
                    case "resource", "resourceId" -> resourceId(r).equals(scope.value());
                    default -> false;
                };
                if (in) {
                    out.add(r);
                }
            }
            return out.stream().mapToInt(Integer::intValue).toArray();
        }

        @Override public int[][] groupTasksByTagPrefix(String prefix) { return new int[0][]; }
        @Override public int candidateCount() { return candTask.length; }
        @Override public int duration(int t) { Integer d = problem.tasks().get(t).duration(); return d == null ? 0 : d; }
        @Override public int demand(int t) { return 1; }
        @Override public int priority(int t) { return problem.tasks().get(t).priority(); }
        @Override public boolean required(int t) { return problem.tasks().get(t).requiredTask(); }
        @Override public long prize(int t) { return required(t) ? 0L : problem.tasks().get(t).effectivePrize(); }
        @Override public int release(int t) { return 0; }
        @Override public int latestEnd(int t) { return horizonTicks(); }

        @Override
        public int due(int t) {
            Instant due = problem.tasks().get(t).dueDate();
            return due == null ? ObjectiveMath.NO_DUE : problem.horizon().toTick(due);
        }

        @Override public int estStart(int t) { return 0; }
        @Override public int lstStart(int t) { return horizonTicks() - duration(t); }
        @Override public int pinStart(int t) { return -1; }
        @Override public int capacity(int r) { return problem.resources().get(r).capacity(); }
        @Override public int maxLoadTicks(int r) { return Integer.MAX_VALUE; }
        @Override public int candidateBegin(int t) { return candBegin[t]; }
        @Override public int candidateEnd(int t) { return candBegin[t + 1]; }
        @Override public int candidateTask(int c) { return candTask[c]; }
        @Override public int candidateResource(int c) { return candResource[c]; }
        @Override public int candidateDuration(int c) { return candDuration[c]; }
        @Override public int candidateAmount(int c) { return 1; }
    }

    /** In-memory solution; tasks are unselected until {@link #place}d. */
    static final class FakeSolution implements SolutionView {
        private final boolean[] selected;
        private final int[] start;
        private final int[] end;
        private final int[][] resources;
        private final int[] slot;
        private final int resourceCount;

        FakeSolution(int taskCount, int resourceCount) {
            this.selected = new boolean[taskCount];
            this.start = new int[taskCount];
            this.end = new int[taskCount];
            this.resources = new int[taskCount][0];
            this.slot = new int[taskCount];
            Arrays.fill(slot, -1);
            this.resourceCount = resourceCount;
        }

        FakeSolution place(int task, int start, int end, int... resourceIdx) {
            this.selected[task] = true;
            this.start[task] = start;
            this.end[task] = end;
            this.resources[task] = resourceIdx.clone();
            return this;
        }

        FakeSolution slot(int task, int slotIdx) {
            this.slot[task] = slotIdx;
            return this;
        }

        @Override public int taskCount() { return selected.length; }
        @Override public boolean selected(int task) { return selected[task]; }
        @Override public int start(int task) { return start[task]; }
        @Override public int end(int task) { return end[task]; }
        @Override public int resourceCount(int task) { return resources[task].length; }
        @Override public int resourceAt(int task, int k) { return resources[task][k]; }
        @Override public String roleAt(int task, int k) { return "default"; }
        @Override public int amountAt(int task, int k) { return 1; }
        @Override public int slotOf(int task) { return slot[task]; }

        @Override
        public int[] tasksOfResource(int resource) {
            List<Integer> tasks = new ArrayList<>();
            for (int t = 0; t < selected.length; t++) {
                if (selected[t] && Arrays.stream(resources[t]).anyMatch(r -> r == resource)) {
                    tasks.add(t);
                }
            }
            tasks.sort((a, b) -> start[a] != start[b] ? Integer.compare(start[a], start[b]) : Integer.compare(a, b));
            return tasks.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    static CheckContext ctx(PlanningProblem problem, CandidateIndexView index, SolutionView solution) {
        return new CheckContext() {
            @Override public PlanningProblem problem() { return problem; }
            @Override public CandidateIndexView index() { return index; }
            @Override public SolutionView solution() { return solution; }
            @Override public long scaled(double value, int scale) { return ScaledMath.scale(value, scale); }
        };
    }

    /** Collects the issues of {@code family.validate}. */
    static final class RecordingValidation implements ValidationContext {
        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        private final PlanningProblem problem;

        RecordingValidation(PlanningProblem problem) {
            this.problem = problem;
        }

        @Override public PlanningProblem problem() { return problem; }
        @Override public CandidateIndexView index() { throw new UnsupportedOperationException(); }
        @Override public <P> P params(Constraint c, Class<P> type) { return null; }
        @Override public void error(String code, String path, String message) { errors.add(code + "@" + path); }
        @Override public void warning(String code, String path, String message) { warnings.add(code + "@" + path); }
        @Override public boolean hasTask(String id) { return problem.tasks().stream().anyMatch(t -> t.id().equals(id)); }
        @Override public boolean hasResource(String id) { return problem.resources().stream().anyMatch(r -> r.id().equals(id)); }
        @Override public boolean hasSlot(String id) { return problem.slots().stream().anyMatch(s -> s.id().equals(id)); }

        @Override
        public int tick(Instant t, String path) {
            try {
                return problem.horizon().toTick(t);
            } catch (RuntimeException e) {
                errors.add("TIME@" + path);
                return -1;
            }
        }
    }
}

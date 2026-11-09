package eu.isygoit.solver.family;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Task;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import eu.isygoit.util.ParamsBinder;
import eu.isygoit.util.ScaledMath;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Test kit of the WP4b family tests: builders for the request records and plain-Java fakes of the SPI views
 * ({@link View} = {@link CandidateIndexView}, {@link Solution} = {@link SolutionView}, {@link Validation},
 * {@link Sink}), so that the families (validate / estimate / preprocess / checker) are tested without any solver. Also reused
 * by the contributor tests, where a Mockito mock of the final {@code CandidateIndex} delegates to {@link View}.
 */
public final class FamilyKit {

    /** Start of every test horizon (a Monday, UTC). */
    public static final Instant START = Instant.parse("2026-09-07T00:00:00Z");

    private FamilyKit() {
    }

    // ------------------------------------------------------------------ builders

    public static Horizon horizon(int ticks, int tickSeconds) {
        return new Horizon(START, START.plusSeconds((long) ticks * tickSeconds), tickSeconds);
    }

    public static Task task(String id, int duration, String... tags) {
        return task(id, duration, List.of(), tags);
    }

    public static Task task(String id, int duration, List<ResourceRequirement> requirements, String... tags) {
        return new Task(id, null, null, duration, null, null, null, null, null, requirements, null, null, null, null, null,
                null, null, new LinkedHashSet<>(Arrays.asList(tags)), null, null);
    }

    public static Resource resource(String id, int capacity, String... tags) {
        return new Resource(id, null, null, capacity, null, new LinkedHashSet<>(Arrays.asList(tags)), null, null, null,
                null, null, null, null);
    }

    public static Constraint constraint(String id, String type, IEnumEnforcement.Types enforcement, ConstraintScope scope,
                                        int weight, Map<String, Object> params) {
        return new Constraint(id, type, enforcement, scope, params, null, weight, null);
    }

    public static Constraint hard(String id, String type, Map<String, Object> params) {
        return constraint(id, type, IEnumEnforcement.Types.STRICTLY_REQUIRED, null, 0, params);
    }

    public static Constraint soft(String id, String type, int weight, Map<String, Object> params) {
        return constraint(id, type, IEnumEnforcement.Types.SOFT, null, weight, params);
    }

    public static PlanningProblem problem(Horizon horizon, String timezone, List<Resource> resources, List<Task> tasks,
                                          List<Constraint> constraints) {
        return new PlanningProblem(1L, "test", timezone, horizon, null, null, null, resources, tasks, constraints, null,
                null, null);
    }

    public static CheckContext check(PlanningProblem problem, CandidateIndexView view, SolutionView solution) {
        return new CheckContext() {
            public PlanningProblem problem() {
                return problem;
            }

            public CandidateIndexView index() {
                return view;
            }

            public SolutionView solution() {
                return solution;
            }

            public long scaled(double value, int scale) {
                return ScaledMath.scale(value, scale);
            }
        };
    }

    // ------------------------------------------------------------------ candidate index fake

    /** Plain fake of the index view; candidates are declared with {@code candidate(..)} and frozen on first read. */
    public static final class View implements CandidateIndexView {
        private final PlanningProblem problem;
        private final PlanLimitsProperties limits = new PlanLimitsProperties();
        private final List<int[]> declared = new ArrayList<>();   // {task, resource, duration, reqOrdinal}
        private final List<String> declaredRoles = new ArrayList<>();
        private final int[] est;
        private final int[] lst;
        private int[] taskBegin;
        private int[] candTask;
        private int[] candResource;
        private int[] candDuration;
        private int[] candReq;
        private String[] candRole;
        private String[] roles;
        private int[] reqRole;

        public View(PlanningProblem problem) {
            this.problem = problem;
            int horizonTicks = (int) problem.horizon().horizonTicks();
            est = new int[problem.tasks().size()];
            lst = new int[problem.tasks().size()];
            for (int t = 0; t < est.length; t++) {
                lst[t] = horizonTicks - problem.tasks().get(t).duration();
            }
        }

        public static View of(PlanningProblem problem) {
            return new View(problem);
        }

        /** Adds a candidate with the task duration and role "default". */
        public View candidate(String task, String resource) {
            return candidate(task, resource, "default");
        }

        public View candidate(String task, String resource, String role) {
            int t = taskIdx(task);
            declared.add(new int[]{t, resourceIdx(resource), problem.tasks().get(t).duration(), 0});
            declaredRoles.add(role);
            taskBegin = null;
            return this;
        }

        /** Every task can use every resource. */
        public View allCandidates() {
            for (Task t : problem.tasks()) {
                for (Resource r : problem.resources()) {
                    candidate(t.id(), r.id());
                }
            }
            return this;
        }

        public View window(String task, int earliest, int latest) {
            est[taskIdx(task)] = earliest;
            lst[taskIdx(task)] = latest;
            return this;
        }

        public View pinned(String task, int start) {
            return window(task, start, start);
        }

        public View limits(long maxPairwise) {
            limits.setMaxPairwise((int) maxPairwise);
            return this;
        }

        public View maxSetupArcs(long maxSetupArcs) {
            limits.setMaxSetupArcs((int) maxSetupArcs);
            return this;
        }

        public PlanningProblem problem() {
            return problem;
        }

        public String[] roles() {
            freeze();
            return roles;
        }

        public int[] reqRole() {
            freeze();
            return reqRole;
        }

        public int[] candReq() {
            freeze();
            return candReq;
        }

        public String roleOf(int candidate) {
            freeze();
            return candRole[candidate];
        }

        private void freeze() {
            if (taskBegin != null) {
                return;
            }
            Integer[] order = new Integer[declared.size()];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            Arrays.sort(order, Comparator.comparingInt((Integer i) -> declared.get(i)[0]).thenComparingInt(i -> i));
            int c = order.length;
            candTask = new int[c];
            candResource = new int[c];
            candDuration = new int[c];
            candRole = new String[c];
            candReq = new int[c];
            Map<String, Integer> reqIds = new TreeMap<>();
            List<String> reqRoleNames = new ArrayList<>();
            List<String> roleNames = new ArrayList<>();
            for (int k = 0; k < c; k++) {
                int[] d = declared.get(order[k]);
                candTask[k] = d[0];
                candResource[k] = d[1];
                candDuration[k] = d[2];
                candRole[k] = declaredRoles.get(order[k]);
                String key = d[0] + "|" + candRole[k];
                Integer req = reqIds.get(key);
                if (req == null) {
                    req = reqRoleNames.size();
                    reqIds.put(key, req);
                    reqRoleNames.add(candRole[k]);
                }
                candReq[k] = req;
                if (!roleNames.contains(candRole[k])) {
                    roleNames.add(candRole[k]);
                }
            }
            roles = roleNames.toArray(new String[0]);
            reqRole = new int[reqRoleNames.size()];
            for (int q = 0; q < reqRole.length; q++) {
                reqRole[q] = roleNames.indexOf(reqRoleNames.get(q));
            }
            int tasks = problem.tasks().size();
            taskBegin = new int[tasks + 1];
            for (int k = 0; k < c; k++) {
                taskBegin[candTask[k] + 1]++;
            }
            for (int t = 0; t < tasks; t++) {
                taskBegin[t + 1] += taskBegin[t];
            }
        }

        @Override
        public int taskCount() {
            return problem.tasks().size();
        }

        @Override
        public int resourceCount() {
            return problem.resources().size();
        }

        @Override
        public int slotCount() {
            return 0;
        }

        @Override
        public Horizon horizon() {
            return problem.horizon();
        }

        @Override
        public int horizonTicks() {
            return (int) problem.horizon().horizonTicks();
        }

        @Override
        public PlanLimitsProperties limits() {
            return limits;
        }

        @Override
        public String taskId(int t) {
            return problem.tasks().get(t).id();
        }

        @Override
        public String resourceId(int r) {
            return problem.resources().get(r).id();
        }

        @Override
        public String slotId(int s) {
            throw new IndexOutOfBoundsException("no slots");
        }

        @Override
        public int taskIdx(String id) {
            for (int t = 0; t < problem.tasks().size(); t++) {
                if (problem.tasks().get(t).id().equals(id)) {
                    return t;
                }
            }
            return -1;
        }

        @Override
        public int resourceIdx(String id) {
            for (int r = 0; r < problem.resources().size(); r++) {
                if (problem.resources().get(r).id().equals(id)) {
                    return r;
                }
            }
            return -1;
        }

        @Override
        public int slotIdx(String id) {
            return -1;
        }

        @Override
        public int[] resolveTasks(ConstraintScope scope) {
            List<Integer> out = new ArrayList<>();
            for (int t = 0; t < taskCount(); t++) {
                Task task = problem.tasks().get(t);
                boolean in = scope == null || "all".equals(scope.kind())
                        || ("task".equals(scope.kind()) && task.id().equals(scope.value()))
                        || ("taskType".equals(scope.kind()) && scope.value() != null && scope.value().equals(task.typeId()))
                        || ("tag".equals(scope.kind()) && task.tags().contains(scope.value()));
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
                Resource res = problem.resources().get(r);
                boolean in = scope == null || "all".equals(scope.kind())
                        || (("resource".equals(scope.kind()) || "resourceId".equals(scope.kind()))
                        && res.id().equals(scope.value()))
                        || ("resourceType".equals(scope.kind()) && scope.value() != null && scope.value().equals(res.typeId()))
                        || ("tag".equals(scope.kind()) && res.tags().contains(scope.value()));
                if (in) {
                    out.add(r);
                }
            }
            return out.stream().mapToInt(Integer::intValue).toArray();
        }

        @Override
        public int[][] groupTasksByTagPrefix(String prefix) {
            Map<String, List<Integer>> groups = new TreeMap<>();
            for (int t = 0; t < taskCount(); t++) {
                for (String tag : problem.tasks().get(t).tags()) {
                    if (tag.startsWith(prefix)) {
                        groups.computeIfAbsent(tag, k -> new ArrayList<>()).add(t);
                    }
                }
            }
            return groups.values().stream()
                    .map(l -> l.stream().mapToInt(Integer::intValue).toArray()).toArray(int[][]::new);
        }

        @Override
        public int candidateCount() {
            freeze();
            return candTask.length;
        }

        @Override
        public int duration(int task) {
            return problem.tasks().get(task).duration();
        }

        @Override
        public int demand(int task) {
            return 1;
        }

        @Override
        public int priority(int task) {
            return 0;
        }

        @Override
        public boolean required(int task) {
            return true;
        }

        @Override
        public long prize(int task) {
            return 0;
        }

        @Override
        public int release(int task) {
            return est[task];
        }

        @Override
        public int latestEnd(int task) {
            return lst[task] + duration(task);
        }

        @Override
        public int due(int task) {
            return Integer.MIN_VALUE;
        }

        @Override
        public int estStart(int task) {
            return est[task];
        }

        @Override
        public int lstStart(int task) {
            return lst[task];
        }

        @Override
        public int pinStart(int task) {
            return est[task] == lst[task] ? est[task] : -1;
        }

        @Override
        public int capacity(int resource) {
            return problem.resources().get(resource).capacity();
        }

        @Override
        public int maxLoadTicks(int resource) {
            return Integer.MAX_VALUE;
        }

        @Override
        public int candidateBegin(int task) {
            freeze();
            return taskBegin[task];
        }

        @Override
        public int candidateEnd(int task) {
            freeze();
            return taskBegin[task + 1];
        }

        @Override
        public int candidateTask(int candidate) {
            freeze();
            return candTask[candidate];
        }

        @Override
        public int candidateResource(int candidate) {
            freeze();
            return candResource[candidate];
        }

        @Override
        public int candidateDuration(int candidate) {
            freeze();
            return candDuration[candidate];
        }

        @Override
        public int candidateAmount(int candidate) {
            return 1;
        }
    }

    // ------------------------------------------------------------------ solution fake

    /** Hand-built solution: tasks are selected by {@code assign}. */
    public static final class Solution implements SolutionView {
        private final View view;
        private final int[] start;
        private final int[] end;
        private final boolean[] selected;
        private final List<List<int[]>> resources = new ArrayList<>();
        private final List<List<String>> roles = new ArrayList<>();

        public Solution(View view) {
            this.view = view;
            int n = view.taskCount();
            start = new int[n];
            end = new int[n];
            selected = new boolean[n];
            for (int t = 0; t < n; t++) {
                resources.add(new ArrayList<>());
                roles.add(new ArrayList<>());
            }
        }

        /** Assigns the task with role "default" to the named resources. */
        public Solution assign(String task, int startTick, int endTick, String... resourceIds) {
            int t = view.taskIdx(task);
            selected[t] = true;
            start[t] = startTick;
            end[t] = endTick;
            for (String r : resourceIds) {
                resources.get(t).add(new int[]{view.resourceIdx(r), 1});
                roles.get(t).add("default");
            }
            return this;
        }

        /** Selects the task without any resource yet (see {@link #addResource}). */
        public Solution select(String task, int startTick, int endTick) {
            int t = view.taskIdx(task);
            selected[t] = true;
            start[t] = startTick;
            end[t] = endTick;
            return this;
        }

        /** Adds one more resource under a given role to an already assigned task. */
        public Solution addResource(String task, String role, String resourceId) {
            int t = view.taskIdx(task);
            resources.get(t).add(new int[]{view.resourceIdx(resourceId), 1});
            roles.get(t).add(role);
            return this;
        }

        @Override
        public int taskCount() {
            return selected.length;
        }

        @Override
        public boolean selected(int task) {
            return selected[task];
        }

        @Override
        public int start(int task) {
            return start[task];
        }

        @Override
        public int end(int task) {
            return end[task];
        }

        @Override
        public int resourceCount(int task) {
            return resources.get(task).size();
        }

        @Override
        public int resourceAt(int task, int k) {
            return resources.get(task).get(k)[0];
        }

        @Override
        public String roleAt(int task, int k) {
            return roles.get(task).get(k);
        }

        @Override
        public int amountAt(int task, int k) {
            return resources.get(task).get(k)[1];
        }

        @Override
        public int[] tasksOfResource(int resource) {
            List<Integer> out = new ArrayList<>();
            for (int t = 0; t < selected.length; t++) {
                if (selected[t] && resources.get(t).stream().anyMatch(a -> a[0] == resource)) {
                    out.add(t);
                }
            }
            out.sort(Comparator.comparingInt((Integer t) -> start[t]).thenComparingInt(t -> t));
            return out.stream().mapToInt(Integer::intValue).toArray();
        }

        @Override
        public int slotOf(int task) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ validation + sink

    /** Collecting validation context. */
    public static final class Validation implements ValidationContext {
        private final View view;
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        public Validation(View view) {
            this.view = view;
        }

        public boolean hasError(String code) {
            return errors.stream().anyMatch(e -> e.startsWith(code + "@"));
        }

        @Override
        public PlanningProblem problem() {
            return view.problem();
        }

        @Override
        public CandidateIndexView index() {
            return view;
        }

        @Override
        public <P> P params(Constraint c, Class<P> type) {
            return ParamsBinder.bind(c.params(), type);
        }

        @Override
        public void error(String code, String path, String message) {
            errors.add(code + "@" + path);
        }

        @Override
        public void warning(String code, String path, String message) {
            warnings.add(code + "@" + path);
        }

        @Override
        public boolean hasTask(String id) {
            return view.taskIdx(id) >= 0;
        }

        @Override
        public boolean hasResource(String id) {
            return view.resourceIdx(id) >= 0;
        }

        @Override
        public boolean hasSlot(String id) {
            return false;
        }

        @Override
        public int tick(Instant t, String path) {
            try {
                return view.horizon().toTick(t);
            } catch (RuntimeException e) {
                errors.add("TIME_NOT_ALIGNED@" + path);
                return -1;
            }
        }
    }

    /** Collecting violation sink. */
    public static final class Sink implements ViolationSink {
        public record Violation(String taskId, String resourceId, String code, String message, long measure) {
        }

        public final List<Violation> violations = new ArrayList<>();

        @Override
        public void violation(String taskId, String resourceId, String code, String message, long measure) {
            violations.add(new Violation(taskId, resourceId, code, message, measure));
        }

        public int count() {
            return violations.size();
        }

        public long measure() {
            return violations.stream().mapToLong(Violation::measure).sum();
        }
    }

    /** Runs the family checker on a constraint. */
    public static Sink run(eu.isygoit.solver.spi.ConstraintFamily family, PlanningProblem problem, View view,
                           Solution solution, Constraint constraint) {
        Object params = ParamsBinder.bind(constraint.params(), family.paramsType());
        Sink sink = new Sink();
        family.checker().check(check(problem, view, solution), constraint, params, sink);
        return sink;
    }

    /** Binds the params of a constraint with the family's type. */
    public static Object bind(eu.isygoit.solver.spi.ConstraintFamily family, Constraint constraint) {
        return ParamsBinder.bind(constraint.params(), family.paramsType());
    }

    /** Builds a params map from alternating keys and values. */
    public static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            out.put((String) keyValues[i], keyValues[i + 1]);
        }
        return out;
    }

    public static Set<String> setOf(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}

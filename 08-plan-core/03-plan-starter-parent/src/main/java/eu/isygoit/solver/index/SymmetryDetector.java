package eu.isygoit.solver.index;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Preference;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.CompiledConstraint;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detects interchangeable tasks and resources for symmetry breaking (design 5.4 step 7, 6.7).
 * <p>
 * A task is a candidate only when nothing singles it out: no pin, no predecessor or successor, no preferences, not part
 * of the initial solution, not named by a constraint/objective scope other than {@code all} and not matched by any
 * string value of a constraint/objective params map (task ids, task or resource tags by prefix, type ids, resource ids;
 * a conservative generic scan, so tag-group families such as {@code no_overlap.groupByTagPrefix} disable the symmetry of
 * the tasks they group). Candidate tasks are interchangeable when their canonical signatures are equal: duration,
 * demand, priority, required, prize, windows, due date, domains, setup class, type, tags, requirements and the surviving
 * candidate list with durations, slots. Resources are interchangeable when they are never referenced by id (selectors,
 * pins, preferences, initial solution, scopes, params) and agree on capacity, availability, skills, tags, type, costs,
 * setup matrix and load cap.
 * <p>
 * Output class ids are dense; {@code -1} marks a unique entity. Equal ids are interchangeable and, inside a class,
 * ordered by index. O(T + R + C) plus the size of the params maps. Stateless and thread-safe.
 */
final class SymmetryDetector {

    /** Task and resource classes. */
    record Result(int[] taskClass, int[] resourceClass) {
    }

    private static final int MAX_SCAN_DEPTH = 8;

    private SymmetryDetector() {
    }

    static Result detect(PlanningProblem problem, CandidateIndexData d, List<CompiledConstraint> constraints) {
        BitSet refTasks = new BitSet(d.taskCount);
        BitSet refResources = new BitSet(d.resourceCount);
        collectReferences(problem, d, constraints, refTasks, refResources);
        return new Result(taskClasses(problem, d, refTasks), resourceClasses(problem, d, refResources));
    }

    // ------------------------------------------------------------------------------------------ references

    private static void collectReferences(PlanningProblem problem, CandidateIndexData d,
                                          List<CompiledConstraint> constraints, BitSet refTasks, BitSet refResources) {
        ScopeResolver scopes = d.scopes;
        List<Task> tasks = problem.tasks();
        for (int t = 0; t < tasks.size(); t++) {
            Task task = tasks.get(t);
            if (task.pin() != null || !task.predecessors().isEmpty() || !task.preferences().isEmpty()) {
                refTasks.set(t);
            }
            for (PrecedenceRef ref : task.predecessors()) {
                int p = scopes.taskIdx(ref.taskId());
                if (p >= 0) {
                    refTasks.set(p);
                }
            }
            if (task.pin() != null) {
                markResources(scopes, task.pin().resourceIds(), refResources);
            }
            for (Preference preference : task.preferences()) {
                int r = scopes.resourceIdx(preference.resourceId());
                if (r >= 0) {
                    refResources.set(r);
                }
            }
            for (String id : task.resourceDurations().keySet()) {
                int r = scopes.resourceIdx(id);
                if (r >= 0) {
                    refResources.set(r);
                }
            }
            for (var requirement : task.requirements()) {
                markResources(scopes, requirement.selector().resourceIds(), refResources);
                markResources(scopes, requirement.selector().excludeResourceIds(), refResources);
            }
        }
        for (InitialAssignment a : problem.initialSolution().assignments()) {
            int t = scopes.taskIdx(a.taskId());
            if (t >= 0) {
                refTasks.set(t);
            }
            markResources(scopes, a.resourceIds(), refResources);
        }
        for (CompiledConstraint cc : constraints) {
            Constraint c = cc.constraint();
            markScope(scopes, c.scope(), refTasks, refResources);
            scan(scopes, c.params(), refTasks, refResources, 0);
        }
        for (Objective o : problem.objectives()) {
            markScope(scopes, o.scope(), refTasks, refResources);
            scan(scopes, o.params(), refTasks, refResources, 0);
        }
    }

    private static void markResources(ScopeResolver scopes, List<String> ids, BitSet refResources) {
        for (String id : ids) {
            int r = scopes.resourceIdx(id);
            if (r >= 0) {
                refResources.set(r);
            }
        }
    }

    private static void markScope(ScopeResolver scopes, ConstraintScope scope, BitSet refTasks, BitSet refResources) {
        if (scope == null || ConstraintScope.ALL.equals(scope.kind())) {
            return;
        }
        for (int t : scopes.resolveTasks(scope)) {
            refTasks.set(t);
        }
        for (int r : scopes.resolveResources(scope)) {
            refResources.set(r);
        }
    }

    private static void scan(ScopeResolver scopes, Object value, BitSet refTasks, BitSet refResources, int depth) {
        if (value == null || depth > MAX_SCAN_DEPTH) {
            return;
        }
        if (value instanceof String s) {
            markString(scopes, s, refTasks, refResources);
        } else if (value instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                scan(scopes, v, refTasks, refResources, depth + 1);
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object v : collection) {
                scan(scopes, v, refTasks, refResources, depth + 1);
            }
        }
    }

    private static void markString(ScopeResolver scopes, String s, BitSet refTasks, BitSet refResources) {
        if (s.isEmpty()) {
            return;
        }
        int t = scopes.taskIdx(s);
        if (t >= 0) {
            refTasks.set(t);
        }
        int r = scopes.resourceIdx(s);
        if (r >= 0) {
            refResources.set(r);
        }
        for (int i : scopes.tasksWithTagPrefix(s)) {
            refTasks.set(i);
        }
        for (int i : scopes.resourcesWithTagPrefix(s)) {
            refResources.set(i);
        }
        for (int i : scopes.tasksOfType(s)) {
            refTasks.set(i);
        }
        for (int i : scopes.resourcesOfType(s)) {
            refResources.set(i);
        }
    }

    // ------------------------------------------------------------------------------------------ classes

    private static int[] taskClasses(PlanningProblem problem, CandidateIndexData d, BitSet referenced) {
        int n = d.taskCount;
        Map<Signature, IntList> groups = new HashMap<>();
        for (int t = 0; t < n; t++) {
            if (referenced.get(t)) {
                continue;
            }
            Task task = problem.tasks().get(t);
            groups.computeIfAbsent(taskSignature(d, t, task), k -> new IntList()).add(t);
        }
        return assign(n, groups);
    }

    private static Signature taskSignature(CandidateIndexData d, int t, Task task) {
        IntList data = new IntList(32);
        data.add(d.duration[t]);
        data.add(d.demand[t]);
        data.add(d.priority[t]);
        data.add(d.required[t] ? 1 : 0);
        data.add((int) (d.prize[t] >>> 32));
        data.add((int) d.prize[t]);
        data.add(d.release[t]);
        data.add(d.latestEnd[t]);
        data.add(d.due[t]);
        data.add(d.estStart[t]);
        data.add(d.lstStart[t]);
        data.add(d.setupClass[t]);
        for (int q = d.taskReqStart[t]; q < d.taskReqStart[t + 1]; q++) {
            data.add(d.reqCount[q]);
            data.add(d.reqAmount[q]);
            data.add(d.reqRole[q]);
            for (int c = d.reqCandStart[q]; c < d.reqCandStart[q + 1]; c++) {
                data.add(d.candResource[c]);
                data.add(d.candDuration[c]);
                data.add(d.candForced[c] ? 1 : 0);
                for (int k = d.candStartRunStart[c]; k < d.candStartRunStart[c + 1]; k++) {
                    data.add(d.candStartRuns[k]);
                }
                data.add(-2);
            }
            data.add(-1);
        }
        for (int k = d.taskSlotStart[t]; k < d.taskSlotStart[t + 1]; k++) {
            data.add(d.taskSlots[k]);
        }
        data.add(-3);
        for (int k = d.taskStartRunStart[t]; k < d.taskStartRunStart[t + 1]; k++) {
            data.add(d.taskStartRuns[k]);
        }
        return new Signature(data.toArray(), task.typeId(), task.tags(), null);
    }

    private static int[] resourceClasses(PlanningProblem problem, CandidateIndexData d, BitSet referenced) {
        int n = d.resourceCount;
        Map<Signature, IntList> groups = new HashMap<>();
        for (int r = 0; r < n; r++) {
            if (referenced.get(r)) {
                continue;
            }
            Resource resource = problem.resources().get(r);
            IntList data = new IntList(16);
            data.add(d.capacity[r]);
            data.add(d.resCalendar[r]);
            data.add(d.maxLoadTicks[r]);
            data.add(d.resSetupMatrix[r]);
            data.add(d.resNeverAvailable[r] ? 1 : 0);
            for (int k = d.resRunStart[r]; k < d.resRunStart[r + 1]; k++) {
                data.add(d.resRuns[k]);
            }
            Signature sig = new Signature(data.toArray(), resource.typeId(),
                    resource.skills(), resource.tags(), resource.costPerTick(), resource.costPerUse());
            groups.computeIfAbsent(sig, k -> new IntList()).add(r);
        }
        return assign(n, groups);
    }

    private static int[] assign(int n, Map<Signature, IntList> groups) {
        int[] classes = new int[n];
        Arrays.fill(classes, -1);
        // deterministic class ids: ordered by the smallest member index
        int next = 0;
        int[][] byFirst = new int[n][];
        for (IntList members : groups.values()) {
            if (members.size() >= 2) {
                byFirst[members.get(0)] = members.toArray();
            }
        }
        for (int first = 0; first < n; first++) {
            if (byFirst[first] != null) {
                for (int member : byFirst[first]) {
                    classes[member] = next;
                }
                next++;
            }
        }
        return classes;
    }

    /** Canonical signature with value equality (arrays compared element-wise). */
    private static final class Signature {
        private final int[] data;
        private final Object[] extras;
        private final int hash;

        Signature(int[] data, Object... extras) {
            this.data = data;
            this.extras = extras;
            this.hash = 31 * Arrays.hashCode(data) + Arrays.deepHashCode(extras);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Signature other) || other.hash != hash) {
                return false;
            }
            return Arrays.equals(data, other.data) && Objects.deepEquals(extras, other.extras);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}

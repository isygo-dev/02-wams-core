package eu.isygoit.solver.diag;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.check.SelectorMatcher;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.index.CandidateIndexBuilder;
import eu.isygoit.solver.spi.CandidateIndexView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Phase A of the infeasibility explanation (design 6.10): exact, engine-neutral structural infeasibility proofs on the
 * compiled index, BEFORE any solver runs (so it also protects Timefold).
 * <p>
 * Findings (each is a proof; a non-empty result means INFEASIBLE):
 * <ol>
 *   <li>a required task with no candidate for a requirement ({@code NO_ELIGIBLE_RESOURCE}, the message lists why:
 *       selector/skills, capacity amount, pin, pruning constraints, calendar/window);</li>
 *   <li>fewer candidates than the requirement {@code count} ({@code REQUIREMENT_COUNT});</li>
 *   <li>an empty start domain ({@code WINDOW_EMPTY}: window shorter than the duration, calendars, slots);</li>
 *   <li>critical path: the earliest end of a required task along precedence chains exceeds its latest end
 *       ({@code CRITICAL_PATH_TOO_LONG} task plus the {@code EDGE} items of the chain; only the root cause of a chain is
 *       reported);</li>
 *   <li>single-resource energy: forced work of a resource exceeds its available capacity inside the joint window of
 *       the forced tasks, or its {@code maxLoadTicks} ({@code CAPACITY_EXCEEDED});</li>
 *   <li>a pin or locked assignment that is not a candidate / lies outside the start domain
 *       ({@code PIN_CONFLICT}, {@code LOCKED_CONFLICT}).</li>
 * </ol>
 * Every finding is a {@link CoreItem}; the core has {@code method = PRESOLVE_ANALYSIS} and {@code minimal = true} per
 * finding. At most {@value #MAX_FINDINGS} findings are listed ({@code details.truncated} says when more existed).
 * <p>
 * Units are ticks. The analyzer is stateless and thread-safe. It reads a {@link CandidateIndexView}; the requirement
 * structure and the available ticks of a resource, which the view does not expose, come through {@link Structure}
 * (production: adapter over the {@code CandidateIndex} CSR arrays).
 */
public final class PreSolveAnalyzer {

    /** Maximum number of findings reported. */
    public static final int MAX_FINDINGS = 50;

    private static final Set<String> PRUNING_TYPES =
            Set.of("task_eligibility", "fixed_assignment", "time_window", "resource_availability");

    /** Requirement-level data of the compiled index that {@link CandidateIndexView} does not expose. */
    public interface Structure {
        /** @return per requirement of the task: the number of distinct resources it needs ({@code count}) */
        int[] requirementNeeded(int task);

        /** @return per requirement of the task: the number of surviving candidate resources */
        int[] requirementCandidates(int task);

        /** @return global candidate ids that EVERY solution must use (requirements with exactly {@code count} candidates) */
        int[] forcedCandidates(int task);

        /** @return working ticks of the resource inside {@code [lo, hi)} (calendar and unavailable windows applied) */
        long availableTicks(int resource, int lo, int hi);

        /**
         * Why candidates of the requirement were pruned at compile time (design 6.10 A item 1).
         *
         * @param task        task index
         * @param requirement requirement position inside the task (0-based)
         * @return distinct pairs {@code {reasonCode, constraintId}} ({@code constraintId} empty for structural reasons);
         * empty when unknown
         */
        default List<String[]> pruneReasons(int task, int requirement) {
            return List.of();
        }
    }

    /**
     * Analyses a compiled problem.
     *
     * @param problem the compiled problem (index must be present)
     * @return the INFEASIBLE diagnostics when a proof was found, empty otherwise
     */
    public Optional<Diagnostics> analyze(CompiledProblem problem) {
        if (problem.index() == null) {
            return Optional.empty();
        }
        return analyze(problem.problem(), problem.index(), new IndexStructure(problem.index()));
    }

    /**
     * Analyses a problem given its index view and requirement structure.
     *
     * @param problem   the normalized problem
     * @param index     the full index view
     * @param structure requirement-level data
     * @return the INFEASIBLE diagnostics when a proof was found, empty otherwise
     */
    public Optional<Diagnostics> analyze(PlanningProblem problem, CandidateIndexView index, Structure structure) {
        Run run = new Run(problem, index, structure);
        run.execute();
        if (run.items.isEmpty()) {
            return Optional.empty();
        }
        var core = CoreItems.core(CoreItems.METHOD_PRESOLVE, true, run.items);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("phase", "PRESOLVE");
        details.put("findings", run.items.size());
        details.put("truncated", run.truncated);
        String message = "The problem is infeasible before solving: " + run.items.get(0).message()
                + (run.items.size() > 1 ? " (and " + (run.items.size() - 1) + " more finding(s))" : "");
        return Optional.of(CoreItems.diagnostics(message, core, List.of(), details));
    }

    // ------------------------------------------------------------------------------------------------ one run

    private static final class Run {
        final PlanningProblem problem;
        final CandidateIndexView index;
        final Structure structure;
        final int taskCount;
        final Task[] tasks;
        final List<CoreItem> items = new ArrayList<>();
        final Set<Integer> flagged = new HashSet<>();
        boolean truncated;

        Run(PlanningProblem problem, CandidateIndexView index, Structure structure) {
            this.problem = problem;
            this.index = index;
            this.structure = structure;
            this.taskCount = index.taskCount();
            this.tasks = new Task[taskCount];
            for (Task task : problem.tasks()) {
                int t = index.taskIdx(task.id());
                if (t >= 0) {
                    tasks[t] = task;
                }
            }
        }

        void execute() {
            requirementFindings();
            boolean[] covered = criticalPath();
            windowFindings(covered);
            energyFindings();
            pinAndLockFindings();
        }

        boolean add(CoreItem item) {
            if (items.size() >= MAX_FINDINGS) {
                truncated = true;
                return false;
            }
            items.add(item);
            return true;
        }

        // ---- (1) (2)

        void requirementFindings() {
            for (int t = 0; t < taskCount; t++) {
                Task task = tasks[t];
                if (task == null || !task.requiredTask()) {
                    continue;
                }
                int[] need = structure.requirementNeeded(t);
                int[] have = structure.requirementCandidates(t);
                List<ResourceRequirement> reqs = task.effectiveRequirements();
                for (int q = 0; q < need.length && q < have.length; q++) {
                    ResourceRequirement req = q < reqs.size() ? reqs.get(q) : null;
                    String role = req == null ? "#" + q : req.role();
                    if (have[q] == 0) {
                        List<CoreItem> extra = new ArrayList<>();
                        String why = diagnose(t, task, req, q, extra);
                        add(CoreItems.item(CoreItems.TASK, task.id(), CoreItems.NO_ELIGIBLE_RESOURCE,
                                "task '" + task.id() + "' has no eligible resource for role '" + role + "': " + why));
                        extra.forEach(this::add);
                        flagged.add(t);
                    } else if (have[q] < need[q]) {
                        add(CoreItems.item(CoreItems.TASK, task.id(), CoreItems.REQUIREMENT_COUNT,
                                "task '" + task.id() + "' needs " + need[q] + " resource(s) for role '" + role
                                        + "' but only " + have[q] + " candidate(s) remain"));
                        flagged.add(t);
                    }
                }
            }
        }

        String diagnose(int t, Task task, ResourceRequirement req, int q, List<CoreItem> extra) {
            List<String[]> recorded = structure.pruneReasons(t, q);
            if (!recorded.isEmpty()) {
                List<String> parts = new ArrayList<>();
                for (String[] pair : recorded) {
                    boolean hasConstraint = pair[1] != null && !pair[1].isEmpty();
                    parts.add(pair[0] + (hasConstraint ? ":" + pair[1] : ""));
                    if (hasConstraint) {
                        extra.add(CoreItems.item(CoreItems.CONSTRAINT, pair[1], CoreItems.CONSTRAINT_REASON,
                                "constraint '" + pair[1] + "' pruned candidates of task '" + task.id() + "'"));
                    }
                }
                return "all candidates were pruned (" + String.join(", ", parts) + ")";
            }
            if (req == null) {
                return "no candidate remains";
            }
            int amount = req.amount() != null ? req.amount() : task.demand();
            int matched = 0;
            int fit = 0;
            for (Resource resource : problem.resources()) {
                if (SelectorMatcher.matches(req.selector(), resource)) {
                    matched++;
                    if (resource.capacity() >= amount) {
                        fit++;
                    }
                }
            }
            if (matched == 0) {
                boolean skills = !req.selector().skills().isEmpty() || !req.selector().anySkills().isEmpty()
                        || !req.selector().tags().isEmpty();
                return (skills ? "SKILL" : "SELECTOR") + ": no resource matches the requirement selector";
            }
            if (fit == 0) {
                return "CAPACITY_AMOUNT: " + matched + " matching resource(s) have a capacity below the required "
                        + "amount " + amount;
            }
            List<String> reasons = new ArrayList<>();
            if (task.pin() != null && !task.pin().resourceIds().isEmpty()) {
                reasons.add("PIN");
            }
            for (Constraint constraint : problem.constraints()) {
                if (constraint.enforcement() == IEnumEnforcement.Types.SOFT || constraint.type() == null
                        || !PRUNING_TYPES.contains(constraint.type().trim().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                int[] scoped = index.resolveTasks(constraint.scope());
                if (scoped != null && Arrays.binarySearch(scoped, t) >= 0) {
                    reasons.add("ELIGIBILITY:" + constraint.id());
                    extra.add(CoreItems.item(CoreItems.CONSTRAINT, constraint.id(), CoreItems.CONSTRAINT_REASON,
                            "constraint '" + constraint.id() + "' (" + constraint.type()
                                    + ") restricts the candidates of task '" + task.id() + "'"));
                }
            }
            if (reasons.isEmpty()) {
                reasons.add("CALENDAR/WINDOW");
            }
            return String.join(", ", reasons) + ": " + matched + " matching resource(s) were pruned";
        }

        // ---- (4) critical path, returns per task "covered by a path finding"

        boolean[] criticalPath() {
            boolean[] coveredByPath = new boolean[taskCount];
            int[][] predOf = new int[taskCount][];
            PrecedenceRef[][] refOf = new PrecedenceRef[taskCount][];
            int[] indegree = new int[taskCount];
            List<List<Integer>> succs = new ArrayList<>(taskCount);
            for (int t = 0; t < taskCount; t++) {
                succs.add(new ArrayList<>());
            }
            for (int t = 0; t < taskCount; t++) {
                Task task = tasks[t];
                int n = task == null ? 0 : task.predecessors().size();
                int[] preds = new int[n];
                PrecedenceRef[] refs = new PrecedenceRef[n];
                int m = 0;
                for (int i = 0; i < n; i++) {
                    PrecedenceRef ref = task.predecessors().get(i);
                    int p = index.taskIdx(ref.taskId());
                    if (p >= 0 && p != t) {
                        preds[m] = p;
                        refs[m] = ref;
                        m++;
                        succs.get(p).add(t);
                        indegree[t]++;
                    }
                }
                predOf[t] = Arrays.copyOf(preds, m);
                refOf[t] = Arrays.copyOf(refs, m);
            }
            int[] order = new int[taskCount];
            int head = 0;
            int tail = 0;
            for (int t = 0; t < taskCount; t++) {
                if (indegree[t] == 0) {
                    order[tail++] = t;
                }
            }
            while (head < tail) {
                int u = order[head++];
                for (int v : succs.get(u)) {
                    if (--indegree[v] == 0) {
                        order[tail++] = v;
                    }
                }
            }
            if (tail < taskCount) {
                return coveredByPath; // cyclic: rejected by validation, nothing sound to say here
            }
            boolean[] forced = new boolean[taskCount];
            for (int t = 0; t < taskCount; t++) {
                forced[t] = tasks[t] != null && tasks[t].requiredTask();
            }
            for (int i = taskCount - 1; i >= 0; i--) {
                int t = order[i];
                if (forced[t]) {
                    for (int k = 0; k < predOf[t].length; k++) {
                        if (refOf[t][k].presence() == PrecedenceRef.Presence.IMPLIES) {
                            forced[predOf[t][k]] = true;
                        }
                    }
                }
            }
            long[] minDur = new long[taskCount];
            for (int t = 0; t < taskCount; t++) {
                minDur[t] = minDuration(t);
            }
            long[] es = new long[taskCount];
            int[] crit = new int[taskCount];
            boolean[] violating = new boolean[taskCount];
            for (int i = 0; i < taskCount; i++) {
                int t = order[i];
                es[t] = Math.max(0, index.release(t));
                crit[t] = -1;
                if (forced[t]) {
                    for (int k = 0; k < predOf[t].length; k++) {
                        int p = predOf[t][k];
                        if (!forced[p]) {
                            continue;
                        }
                        PrecedenceRef ref = refOf[t][k];
                        long lag = ref.minLag();
                        long candidate = switch (ref.type()) {
                            case FINISH_START -> es[p] + minDur[p] + lag;
                            case START_START -> es[p] + lag;
                            case FINISH_FINISH -> es[p] + minDur[p] + lag - minDur[t];
                            case START_FINISH -> es[p] + lag - minDur[t];
                        };
                        if (candidate > es[t]) {
                            es[t] = candidate;
                            crit[t] = p;
                        }
                    }
                    violating[t] = es[t] + minDur[t] > index.latestEnd(t);
                }
            }
            boolean[] rootWindow = new boolean[taskCount];
            for (int i = 0; i < taskCount; i++) {
                int t = order[i];
                if (!violating[t]) {
                    continue;
                }
                if (crit[t] < 0) {
                    rootWindow[t] = true;
                    continue;
                }
                coveredByPath[t] = true;
                if (violating[crit[t]] || flagged.contains(t)) {
                    continue; // the root cause is reported on an earlier task of the chain
                }
                add(CoreItems.item(CoreItems.TASK, tasks[t].id(), CoreItems.CRITICAL_PATH_TOO_LONG,
                        "task '" + tasks[t].id() + "' cannot finish before tick " + (es[t] + minDur[t])
                                + " along its precedence chain but must end by tick " + index.latestEnd(t)));
                int u = t;
                while (crit[u] >= 0) {
                    add(CoreItems.edge(tasks[crit[u]].id(), tasks[u].id(), "precedence edge " + tasks[crit[u]].id()
                            + " -> " + tasks[u].id() + " lies on the critical path"));
                    u = crit[u];
                }
                flagged.add(t);
            }
            this.rootWindow = rootWindow;
            return coveredByPath;
        }

        boolean[] rootWindow;

        long minDuration(int t) {
            int begin = index.candidateBegin(t);
            int end = index.candidateEnd(t);
            if (end <= begin) {
                return index.duration(t);
            }
            long min = Long.MAX_VALUE;
            for (int c = begin; c < end; c++) {
                min = Math.min(min, index.candidateDuration(c));
            }
            return min;
        }

        // ---- (3) windows

        void windowFindings(boolean[] coveredByPath) {
            for (int t = 0; t < taskCount; t++) {
                Task task = tasks[t];
                if (task == null || !task.requiredTask() || flagged.contains(t) || coveredByPath[t]) {
                    continue;
                }
                boolean emptyDomain = index.estStart(t) > index.lstStart(t);
                boolean releaseTooLate = rootWindow != null && rootWindow[t];
                if (emptyDomain || releaseTooLate) {
                    add(CoreItems.item(CoreItems.TASK, task.id(), CoreItems.WINDOW_EMPTY,
                            "task '" + task.id() + "' has no feasible start: duration " + minDuration(t)
                                    + " does not fit between release " + index.release(t) + " and latest end "
                                    + index.latestEnd(t) + " (start domain [" + index.estStart(t) + ","
                                    + index.lstStart(t) + "])"));
                    flagged.add(t);
                }
            }
        }

        // ---- (5) energy

        void energyFindings() {
            int resourceCount = index.resourceCount();
            long[] energy = new long[resourceCount];
            long[] load = new long[resourceCount];
            int[] lo = new int[resourceCount];
            int[] hi = new int[resourceCount];
            Arrays.fill(lo, Integer.MAX_VALUE);
            Arrays.fill(hi, Integer.MIN_VALUE);
            List<List<Integer>> forcedTasks = new ArrayList<>(resourceCount);
            for (int r = 0; r < resourceCount; r++) {
                forcedTasks.add(new ArrayList<>(0));
            }
            for (int t = 0; t < taskCount; t++) {
                Task task = tasks[t];
                if (task == null || !task.requiredTask() || flagged.contains(t)) {
                    continue;
                }
                for (int c : structure.forcedCandidates(t)) {
                    int r = index.candidateResource(c);
                    long duration = index.candidateDuration(c);
                    energy[r] += duration * index.candidateAmount(c);
                    load[r] += duration;
                    lo[r] = Math.min(lo[r], index.estStart(t));
                    hi[r] = Math.max(hi[r], index.latestEnd(t));
                    forcedTasks.get(r).add(t);
                }
            }
            for (int r = 0; r < resourceCount; r++) {
                if (forcedTasks.get(r).isEmpty()) {
                    continue;
                }
                long available = structure.availableTicks(r, Math.max(0, lo[r]), Math.max(lo[r], hi[r]));
                long capacity = (long) index.capacity(r) * available;
                String resourceId = index.resourceId(r);
                boolean over = energy[r] > capacity;
                boolean overLoad = index.maxLoadTicks(r) != Integer.MAX_VALUE && load[r] > index.maxLoadTicks(r);
                if (!over && !overLoad) {
                    continue;
                }
                add(CoreItems.item(CoreItems.RESOURCE, resourceId, CoreItems.CAPACITY_EXCEEDED, over
                        ? "resource '" + resourceId + "' must carry " + energy[r] + " units of work but offers only "
                        + capacity + " inside ticks [" + lo[r] + "," + hi[r] + ")"
                        : "resource '" + resourceId + "' must carry " + load[r] + " ticks but maxLoadTicks is "
                        + index.maxLoadTicks(r)));
                int listed = 0;
                for (int t : forcedTasks.get(r)) {
                    if (listed++ >= 20) {
                        break;
                    }
                    add(CoreItems.item(CoreItems.TASK, tasks[t].id(), CoreItems.CAPACITY_EXCEEDED,
                            "task '" + tasks[t].id() + "' can only run on resource '" + resourceId + "'"));
                }
            }
        }

        // ---- (6) pins and locks

        void pinAndLockFindings() {
            for (int t = 0; t < taskCount; t++) {
                Task task = tasks[t];
                if (task == null || !task.requiredTask() || task.pin() == null || flagged.contains(t)) {
                    continue;
                }
                if (task.pin().start() != null) {
                    int tick = problem.horizon().ceilTick(task.pin().start());
                    if (tick < index.release(t) || tick + minDuration(t) > index.latestEnd(t)) {
                        add(CoreItems.item(CoreItems.TASK, task.id(), CoreItems.PIN_CONFLICT,
                                "task '" + task.id() + "' is pinned at tick " + tick
                                        + " which is outside its window [" + index.release(t) + ","
                                        + index.latestEnd(t) + "]"));
                        flagged.add(t);
                        continue;
                    }
                }
                for (String id : task.pin().resourceIds()) {
                    if (!isCandidate(t, index.resourceIdx(id))) {
                        add(CoreItems.item(CoreItems.TASK, task.id(), CoreItems.PIN_CONFLICT,
                                "task '" + task.id() + "' is pinned to resource '" + id
                                        + "' which is not an eligible/available candidate"));
                        flagged.add(t);
                        break;
                    }
                }
            }
            for (InitialAssignment lock : problem.initialSolution().assignments()) {
                int t = index.taskIdx(lock.taskId());
                if (!lock.locked() || t < 0 || flagged.contains(t)) {
                    continue;
                }
                boolean conflict = false;
                String why = null;
                for (String id : lock.resourceIds()) {
                    if (!isCandidate(t, index.resourceIdx(id))) {
                        conflict = true;
                        why = "resource '" + id + "' is not an eligible/available candidate";
                        break;
                    }
                }
                if (!conflict && lock.start() != null && index.estStart(t) <= index.lstStart(t)) {
                    int tick = problem.horizon().ceilTick(lock.start());
                    if (tick < index.estStart(t) || tick > index.lstStart(t)) {
                        conflict = true;
                        why = "start tick " + tick + " is outside the feasible start domain ["
                                + index.estStart(t) + "," + index.lstStart(t) + "]";
                    }
                }
                if (conflict) {
                    add(CoreItems.item(CoreItems.TASK, lock.taskId(), CoreItems.LOCKED_CONFLICT,
                            "locked assignment of task '" + lock.taskId() + "' is impossible: " + why));
                    flagged.add(t);
                }
            }
        }

        boolean isCandidate(int t, int resource) {
            if (resource < 0) {
                return false;
            }
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                if (index.candidateResource(c) == resource) {
                    return true;
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------ production adapter

    /**
     * {@link Structure} over the CSR arrays of the real {@link CandidateIndex} (design 5.4): requirement ids of task
     * {@code t} are {@code [taskReqStart[t], taskReqStart[t+1])}; {@code reqCandStart} is the CSR of the candidates of each
     * requirement; {@code prunedCandidates} records why candidates were removed; {@code resRunStart/resRuns} hold the flattened available runs {@code [lo,hi)} of each resource (empty
     * range = always available).
     */
    private static final class IndexStructure implements Structure {
        private final CandidateIndex ix;

        IndexStructure(CandidateIndex ix) {
            this.ix = ix;
        }

        @Override
        public int[] requirementNeeded(int task) {
            int base = ix.taskReqStart[task];
            int n = ix.taskReqStart[task + 1] - base;
            int[] out = new int[n];
            for (int q = 0; q < n; q++) {
                out[q] = ix.reqCount[base + q];
            }
            return out;
        }

        @Override
        public int[] requirementCandidates(int task) {
            int base = ix.taskReqStart[task];
            int n = ix.taskReqStart[task + 1] - base;
            int[] out = new int[n];
            for (int q = 0; q < n; q++) {
                out[q] = ix.reqCandStart[base + q + 1] - ix.reqCandStart[base + q];
            }
            return out;
        }

        @Override
        public int[] forcedCandidates(int task) {
            int base = ix.taskReqStart[task];
            int n = ix.taskReqStart[task + 1] - base;
            int[] tmp = new int[ix.taskCandStart[task + 1] - ix.taskCandStart[task]];
            int count = 0;
            for (int q = 0; q < n; q++) {
                int from = ix.reqCandStart[base + q];
                int to = ix.reqCandStart[base + q + 1];
                boolean all = to - from > 0 && to - from == ix.reqCount[base + q];
                for (int c = from; c < to && count < tmp.length; c++) {
                    if (all || ix.candForced[c]) {
                        tmp[count++] = c;
                    }
                }
            }
            return Arrays.copyOf(tmp, count);
        }

        @Override
        public List<String[]> pruneReasons(int task, int requirement) {
            int global = ix.taskReqStart[task] + requirement;
            Map<String, String[]> distinct = new LinkedHashMap<>();
            for (CandidateIndexBuilder.PrunedCandidate pruned : ix.prunedCandidates) {
                if (pruned.task() == task && pruned.requirement() == global) {
                    distinct.putIfAbsent(pruned.reasonCode() + '|' + pruned.constraintId(),
                            new String[]{pruned.reasonCode(), pruned.constraintId()});
                }
            }
            return new ArrayList<>(distinct.values());
        }

        @Override
        public long availableTicks(int resource, int lo, int hi) {
            if (hi <= lo) {
                return 0L;
            }
            int from = ix.resRunStart[resource];
            int to = ix.resRunStart[resource + 1];
            if (from >= to) {
                return hi - lo;
            }
            long sum = 0L;
            for (int i = from; i + 1 < to; i += 2) {
                long a = Math.max(lo, ix.resRuns[i]);
                long b = Math.min(hi, ix.resRuns[i + 1]);
                if (b > a) {
                    sum += b - a;
                }
            }
            return sum;
        }
    }
}

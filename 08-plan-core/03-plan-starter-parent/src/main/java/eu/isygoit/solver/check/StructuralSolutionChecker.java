package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.solver.index.CalendarExpander;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.SolutionView;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Independent re-check of the STRUCTURAL semantics of design 4.2 against a finished solution (design 8.2 step 2).
 * <p>
 * Checked, from the raw {@link PlanningProblem} only (never from the engine's data and never through a solver): every
 * required task present; each requirement fulfilled with {@code count} distinct eligible resources of the right
 * {@code amount <= capacity}; start/end inside the horizon, {@code release <= start}, {@code end <= deadline},
 * {@code end = start + duration of the chosen resource}; start in an allowed slot and {@code end <= slot.end}; start/end
 * inside ONE availability run of every used resource (calendar minus unavailable windows); {@code Task.pin};
 * {@code Task.predecessors} (type, minLag, maxLag, presence); locked assignments; unary resources by a sorted sweep
 * (consecutive intervals must satisfy {@code end_i + setup(i,j) <= start_j}); cumulative resources by an event sweep
 * (load never above capacity); {@code maxLoadTicks}. Catalogue constraints (including the blocks of
 * {@code resource_capacity}) are the families' business and are NOT checked here.
 * <p>
 * Complexity O(T + A + C log C). Units: ticks (tick 0 = horizon start); half-open intervals {@code [start, end)}, zero
 * length tasks never overlap anything. The instance is immutable after construction except for a lazily filled cache of
 * per-resource availability runs; use it from ONE thread (a validation is single threaded).
 */
public final class StructuralSolutionChecker {

    /** Prefix of every structural violation code (design 8.1). */
    public static final String PREFIX = "SOLUTION_INVALID_";

    /** One structural violation; {@code taskId}/{@code resourceId} may be {@code null}. */
    public record Violation(String code, String taskId, String resourceId, String message) {
    }

    private static final int[] ALWAYS = new int[0];

    private final PlanningProblem problem;
    private final CandidateIndexView index;
    private final Horizon horizon;
    private final int horizonTicks;
    private final Task[] tasks;
    private final Resource[] resources;
    private final int[] baseDuration;
    private final int[] releaseTick;
    private final int[] deadlineTick;
    private final int[][] allowedSlots;
    private final int[] slotStartTick;
    private final int[] slotEndTick;
    private final Map<String, Calendar> calendars = new HashMap<>();
    private final Map<String, SetupMatrix> matrices = new HashMap<>();
    private final Map<String, Map<String, Integer>> matrixEntries = new HashMap<>();
    private final int[][] runsCache;
    private final boolean[] runsComputed;

    /**
     * @param problem the normalized problem
     * @param index   read-only index view; only the always-available methods are used (ids, counts, lookups, horizon)
     */
    public StructuralSolutionChecker(PlanningProblem problem, CandidateIndexView index) {
        this.problem = problem;
        this.index = index;
        this.horizon = index.horizon();
        this.horizonTicks = index.horizonTicks();

        this.tasks = new Task[index.taskCount()];
        for (Task task : problem.tasks()) {
            int t = index.taskIdx(task.id());
            if (t >= 0) {
                tasks[t] = task;
            }
        }
        this.resources = new Resource[index.resourceCount()];
        for (Resource resource : problem.resources()) {
            int r = index.resourceIdx(resource.id());
            if (r >= 0) {
                resources[r] = resource;
            }
        }
        for (Calendar calendar : problem.calendars()) {
            calendars.put(calendar.id(), calendar);
        }
        for (SetupMatrix matrix : problem.setupMatrices()) {
            matrices.put(matrix.id(), matrix);
            Map<String, Integer> entries = new HashMap<>();
            for (SetupEntry entry : matrix.entries()) {
                entries.put(entry.from() + '\u0000' + entry.to(), entry.ticks());
            }
            matrixEntries.put(matrix.id(), entries);
        }

        this.slotStartTick = new int[index.slotCount()];
        this.slotEndTick = new int[index.slotCount()];
        for (Slot slot : problem.slots()) {
            int s = index.slotIdx(slot.id());
            if (s >= 0) {
                slotStartTick[s] = horizon.ceilTick(slot.start());
                slotEndTick[s] = horizon.floorTick(slot.end());
            }
        }

        int n = tasks.length;
        this.baseDuration = new int[n];
        this.releaseTick = new int[n];
        this.deadlineTick = new int[n];
        this.allowedSlots = new int[n][];
        for (int t = 0; t < n; t++) {
            Task task = tasks[t];
            if (task == null) {
                baseDuration[t] = -1;
                deadlineTick[t] = horizonTicks;
                continue;
            }
            releaseTick[t] = task.release() == null ? 0 : Math.max(0, horizon.ceilTick(task.release()));
            deadlineTick[t] = task.deadline() == null
                    ? horizonTicks : Math.min(horizonTicks, horizon.floorTick(task.deadline()));
            allowedSlots[t] = resolveSlots(task);
            baseDuration[t] = resolveBaseDuration(task, allowedSlots[t]);
        }
        this.runsCache = new int[resources.length][];
        this.runsComputed = new boolean[resources.length];
    }

    // ------------------------------------------------------------------------------------------------ public API

    /**
     * Checks the result-level records: instants consistent with the ticks and the {@code optional} flag.
     *
     * @param assignments the assignments of the result
     * @return violations (empty when consistent)
     */
    public List<Violation> checkRecords(List<Assignment> assignments) {
        List<Violation> out = new ArrayList<>();
        for (Assignment assignment : assignments) {
            int t = index.taskIdx(assignment.taskId());
            if (t < 0 || tasks[t] == null) {
                continue;
            }
            Task task = tasks[t];
            if (assignment.optional() == task.requiredTask()) {
                out.add(violation("OPTIONAL_FLAG", task.id(), null,
                        "assignment of task '" + task.id() + "' has optional=" + assignment.optional()
                                + " but the task is " + (task.requiredTask() ? "required" : "optional")));
            }
            try {
                if (assignment.start() == null || assignment.end() == null
                        || !assignment.start().equals(horizon.toInstant(assignment.startTick()))
                        || !assignment.end().equals(horizon.toInstant(assignment.endTick()))) {
                    out.add(violation("INSTANT", task.id(), null,
                            "start/end instants of task '" + task.id() + "' do not match ticks "
                                    + assignment.startTick() + "/" + assignment.endTick()));
                }
            } catch (ArithmeticException e) {
                out.add(violation("INSTANT", task.id(), null,
                        "ticks of task '" + task.id() + "' are out of range"));
            }
        }
        return out;
    }

    /**
     * Runs every structural check.
     *
     * @param s the solution view
     * @return all violations found (possibly large when the plan is badly wrong; callers use the first few)
     */
    public List<Violation> check(SolutionView s) {
        List<Violation> out = new ArrayList<>();
        Map<String, InitialAssignment> locked = new HashMap<>();
        for (InitialAssignment ia : problem.initialSolution().assignments()) {
            if (ia.locked()) {
                locked.put(ia.taskId(), ia);
            }
        }
        long[] load = new long[resources.length];
        for (int t = 0; t < tasks.length; t++) {
            Task task = tasks[t];
            if (task == null) {
                continue;
            }
            if (!s.selected(t)) {
                if (task.requiredTask()) {
                    out.add(violation("TASK_MISSING", task.id(), null,
                            "required task '" + task.id() + "' is not assigned"));
                }
                if (locked.containsKey(task.id())) {
                    out.add(violation("LOCKED", task.id(), null,
                            "locked task '" + task.id() + "' is not assigned"));
                }
                continue;
            }
            checkTask(s, t, task, out);
            checkPrecedence(s, t, task, out);
            InitialAssignment lock = locked.get(task.id());
            if (lock != null) {
                checkLocked(s, t, task, lock, out);
            }
            int duration = s.end(t) - s.start(t);
            for (int k = 0; k < s.resourceCount(t); k++) {
                load[s.resourceAt(t, k)] += Math.max(0, duration);
            }
        }
        for (int r = 0; r < resources.length; r++) {
            Resource resource = resources[r];
            if (resource == null) {
                continue;
            }
            if (resource.maxLoadTicks() != null && load[r] > resource.maxLoadTicks()) {
                out.add(violation("MAX_LOAD", null, resource.id(),
                        "resource '" + resource.id() + "' carries " + load[r] + " ticks, above maxLoadTicks "
                                + resource.maxLoadTicks()));
            }
            int[] list = s.tasksOfResource(r);
            if (list.length < 2) {
                continue;
            }
            if (resource.capacity() <= 1) {
                sweepUnary(s, r, resource, list, out);
            } else {
                sweepCumulative(s, r, resource, list, out);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ per task

    private void checkTask(SolutionView s, int t, Task task, List<Violation> out) {
        int start = s.start(t);
        int end = s.end(t);
        String id = task.id();
        if (start < 0 || end > horizonTicks || end < start) {
            out.add(violation("HORIZON", id, null, "task '" + id + "' interval [" + start + "," + end
                    + ") is outside the horizon [0," + horizonTicks + "]"));
        }
        if (start < releaseTick[t]) {
            out.add(violation("WINDOW", id, null,
                    "task '" + id + "' starts at " + start + " before its release " + releaseTick[t]));
        }
        if (end > deadlineTick[t]) {
            out.add(violation("WINDOW", id, null,
                    "task '" + id + "' ends at " + end + " after its deadline " + deadlineTick[t]));
        }
        checkDuration(s, t, task, out);
        checkRequirements(s, t, task, out);
        checkSlot(s, t, task, out);
        checkCalendars(s, t, task, out);
        checkPin(s, t, task, out);
    }

    private void checkDuration(SolutionView s, int t, Task task, List<Violation> out) {
        int expected = baseDuration[t];
        if (s.resourceCount(t) == 1 && !task.resourceDurations().isEmpty()) {
            Integer specific = task.resourceDurations().get(index.resourceId(s.resourceAt(t, 0)));
            if (specific != null) {
                expected = specific;
            }
        }
        int actual = s.end(t) - s.start(t);
        if (expected >= 0 && actual != expected) {
            out.add(violation("DURATION", task.id(), null, "task '" + task.id() + "' lasts " + actual
                    + " ticks but its duration on the chosen resource is " + expected));
        }
    }

    private void checkRequirements(SolutionView s, int t, Task task, List<Violation> out) {
        String id = task.id();
        List<ResourceRequirement> requirements = task.effectiveRequirements();
        int n = s.resourceCount(t);
        Set<String> roles = new HashSet<>();
        for (ResourceRequirement requirement : requirements) {
            roles.add(requirement.role());
            int count = requirement.count();
            int amount = requirement.amount() != null ? requirement.amount() : task.demand();
            int got = 0;
            for (int k = 0; k < n; k++) {
                if (!requirement.role().equals(s.roleAt(t, k))) {
                    continue;
                }
                got++;
                Resource resource = resources[s.resourceAt(t, k)];
                if (resource == null) {
                    continue;
                }
                if (!SelectorMatcher.matches(requirement.selector(), resource)) {
                    out.add(violation("ELIGIBILITY", id, resource.id(), "resource '" + resource.id()
                            + "' is not eligible for role '" + requirement.role() + "' of task '" + id + "'"));
                }
                if (s.amountAt(t, k) != amount) {
                    out.add(violation("AMOUNT", id, resource.id(), "task '" + id + "' consumes "
                            + s.amountAt(t, k) + " of resource '" + resource.id() + "' but the requirement says "
                            + amount));
                }
                if (s.amountAt(t, k) > resource.capacity()) {
                    out.add(violation("AMOUNT", id, resource.id(), "task '" + id + "' needs "
                            + s.amountAt(t, k) + " of resource '" + resource.id() + "' whose capacity is "
                            + resource.capacity()));
                }
            }
            if (got != count) {
                out.add(violation("REQUIREMENT", id, null, "task '" + id + "' has " + got
                        + " resource(s) for role '" + requirement.role() + "', expected " + count));
            }
        }
        Set<Integer> distinct = new HashSet<>();
        for (int k = 0; k < n; k++) {
            if (!roles.contains(s.roleAt(t, k))) {
                out.add(violation("REQUIREMENT", id, index.resourceId(s.resourceAt(t, k)),
                        "task '" + id + "' has a resource for unknown role '" + s.roleAt(t, k) + "'"));
            }
            if (!distinct.add(s.resourceAt(t, k))) {
                out.add(violation("REQUIREMENT", id, index.resourceId(s.resourceAt(t, k)),
                        "task '" + id + "' uses resource '" + index.resourceId(s.resourceAt(t, k)) + "' twice"));
            }
        }
    }

    private void checkSlot(SolutionView s, int t, Task task, List<Violation> out) {
        int[] allowed = allowedSlots[t];
        if (allowed == null) {
            return;
        }
        int start = s.start(t);
        int end = s.end(t);
        for (int slot : allowed) {
            if (slotStartTick[slot] == start && end <= slotEndTick[slot]
                    && (s.slotOf(t) < 0 || s.slotOf(t) == slot)) {
                return;
            }
        }
        out.add(violation("SLOT", task.id(), null, "task '" + task.id() + "' [" + start + "," + end
                + ") does not start at an allowed slot start and end inside that slot"));
    }

    private void checkCalendars(SolutionView s, int t, Task task, List<Violation> out) {
        int start = s.start(t);
        int end = s.end(t);
        for (int k = 0; k < s.resourceCount(t); k++) {
            int r = s.resourceAt(t, k);
            int[] runs = runs(r);
            if (runs == ALWAYS) {
                continue;
            }
            if (!insideOneRun(runs, start, end)) {
                out.add(violation("CALENDAR", task.id(), index.resourceId(r), "task '" + task.id() + "' ["
                        + start + "," + end + ") is not inside one availability window of resource '"
                        + index.resourceId(r) + "'"));
            }
        }
    }

    private void checkPin(SolutionView s, int t, Task task, List<Violation> out) {
        if (task.pin() == null) {
            return;
        }
        if (task.pin().start() != null && horizon.ceilTick(task.pin().start()) != s.start(t)) {
            out.add(violation("PIN", task.id(), null, "task '" + task.id() + "' is pinned at tick "
                    + horizon.ceilTick(task.pin().start()) + " but starts at " + s.start(t)));
        }
        List<String> pinned = task.pin().resourceIds();
        if (pinned.isEmpty()) {
            return;
        }
        int cursor = 0;
        for (ResourceRequirement requirement : task.effectiveRequirements()) {
            for (int j = 0; j < requirement.count() && cursor < pinned.size(); j++) {
                String resourceId = pinned.get(cursor++);
                boolean found = false;
                for (int k = 0; k < s.resourceCount(t); k++) {
                    if (requirement.role().equals(s.roleAt(t, k))
                            && resourceId.equals(index.resourceId(s.resourceAt(t, k)))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    out.add(violation("PIN", task.id(), resourceId, "task '" + task.id()
                            + "' is pinned to resource '" + resourceId + "' for role '" + requirement.role()
                            + "' but is not assigned to it"));
                }
            }
        }
    }

    private void checkPrecedence(SolutionView s, int t, Task task, List<Violation> out) {
        for (PrecedenceRef ref : task.predecessors()) {
            int p = index.taskIdx(ref.taskId());
            if (p < 0) {
                continue;
            }
            if (!s.selected(p)) {
                if (ref.presence() == PrecedenceRef.Presence.IMPLIES) {
                    out.add(violation("PRECEDENCE", task.id(), null, "task '" + task.id()
                            + "' is selected but its IMPLIES predecessor '" + ref.taskId() + "' is not"));
                }
                continue;
            }
            long from = switch (ref.type()) {
                case FINISH_START, FINISH_FINISH -> s.end(p);
                case START_START, START_FINISH -> s.start(p);
            };
            long to = switch (ref.type()) {
                case FINISH_START, START_START -> s.start(t);
                case FINISH_FINISH, START_FINISH -> s.end(t);
            };
            long lag = to - from;
            if (lag < ref.minLag() || (ref.maxLag() != null && lag > ref.maxLag())) {
                out.add(violation("PRECEDENCE", task.id(), null, "edge " + ref.taskId() + " -> " + task.id() + " ("
                        + ref.type() + ") has lag " + lag + ", allowed [" + ref.minLag() + ","
                        + (ref.maxLag() == null ? "inf" : ref.maxLag().toString()) + "]"));
            }
        }
    }

    private void checkLocked(SolutionView s, int t, Task task, InitialAssignment lock, List<Violation> out) {
        if (lock.start() != null && horizon.ceilTick(lock.start()) != s.start(t)) {
            out.add(violation("LOCKED", task.id(), null, "locked task '" + task.id() + "' must start at tick "
                    + horizon.ceilTick(lock.start()) + " but starts at " + s.start(t)));
        }
        if (!lock.resourceIds().isEmpty()) {
            Set<String> assigned = new HashSet<>();
            for (int k = 0; k < s.resourceCount(t); k++) {
                assigned.add(index.resourceId(s.resourceAt(t, k)));
            }
            if (!assigned.equals(new HashSet<>(lock.resourceIds()))) {
                out.add(violation("LOCKED", task.id(), null, "locked task '" + task.id()
                        + "' must use resources " + lock.resourceIds() + " but uses " + assigned));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ sweeps

    /** Unary resource: sorted sweep with setup gaps. {@code list} is sorted by (start, task index). */
    private void sweepUnary(SolutionView s, int r, Resource resource, int[] list, List<Violation> out) {
        SetupMatrix matrix = resource.setupMatrixId() == null ? null : matrices.get(resource.setupMatrixId());
        Map<String, Integer> entries = matrix == null ? null : matrixEntries.get(matrix.id());
        int prev = -1;
        for (int t : list) {
            if (s.end(t) <= s.start(t)) {
                continue; // zero length tasks never conflict
            }
            if (prev >= 0) {
                if (s.start(t) < s.end(prev)) {
                    out.add(violation("OVERLAP", tasks[t].id(), resource.id(), "tasks '" + tasks[prev].id()
                            + "' and '" + tasks[t].id() + "' overlap on unary resource '" + resource.id() + "'"));
                } else if (matrix != null) {
                    int setup = setupTicks(matrix, entries, tasks[prev].setupClass(), tasks[t].setupClass());
                    if ((long) s.start(t) < (long) s.end(prev) + setup) {
                        out.add(violation("SETUP", tasks[t].id(), resource.id(), "gap between '" + tasks[prev].id()
                                + "' and '" + tasks[t].id() + "' on resource '" + resource.id() + "' is "
                                + (s.start(t) - s.end(prev)) + " ticks but the setup needs " + setup));
                    }
                }
            }
            if (prev < 0 || s.end(t) >= s.end(prev)) {
                prev = t;
            }
        }
    }

    private static int setupTicks(SetupMatrix matrix, Map<String, Integer> entries, String from, String to) {
        if (from == null || to == null) {
            return 0;
        }
        if (from.equals(to)) {
            return matrix.sameClassTicks();
        }
        Integer ticks = entries == null ? null : entries.get(from + '\u0000' + to);
        return ticks != null ? ticks : matrix.defaultTicks();
    }

    /** Cumulative resource: event sweep, ends are released before starts at the same tick. */
    private void sweepCumulative(SolutionView s, int r, Resource resource, int[] list, List<Violation> out) {
        int n = 0;
        int[] members = new int[list.length];
        int[] amounts = new int[list.length];
        for (int t : list) {
            if (s.end(t) <= s.start(t)) {
                continue;
            }
            int amount = 0;
            for (int k = 0; k < s.resourceCount(t); k++) {
                if (s.resourceAt(t, k) == r) {
                    amount += s.amountAt(t, k);
                }
            }
            members[n] = t;
            amounts[n] = amount;
            n++;
        }
        // ends sorted by (end, local index)
        long[] ends = new long[n];
        for (int i = 0; i < n; i++) {
            ends[i] = (((long) s.end(members[i])) << 32) | (i & 0xFFFFFFFFL);
        }
        Arrays.sort(ends);
        long current = 0;
        int endPos = 0;
        int capacity = resource.capacity();
        boolean reported = false;
        for (int i = 0; i < n && !reported; i++) { // members are already ascending by start
            int start = s.start(members[i]);
            while (endPos < n && (int) (ends[endPos] >> 32) <= start) {
                current -= amounts[(int) ends[endPos]];
                endPos++;
            }
            current += amounts[i];
            if (current > capacity) {
                out.add(violation("CAPACITY", tasks[members[i]].id(), resource.id(), "load " + current
                        + " exceeds capacity " + capacity + " of resource '" + resource.id() + "' at tick "
                        + start));
                reported = true; // one violation per resource is enough to reject; avoids a flood
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private int[] resolveSlots(Task task) {
        if (task.startSlots().isEmpty() || index.slotCount() == 0) {
            return null;
        }
        for (String id : task.startSlots()) {
            if ("ALL".equalsIgnoreCase(id)) {
                int[] all = new int[index.slotCount()];
                for (int i = 0; i < all.length; i++) {
                    all[i] = i;
                }
                return all;
            }
        }
        int[] tmp = new int[task.startSlots().size()];
        int n = 0;
        for (String id : task.startSlots()) {
            int slot = index.slotIdx(id);
            if (slot >= 0) {
                tmp[n++] = slot;
            }
        }
        return Arrays.copyOf(tmp, n);
    }

    private int resolveBaseDuration(Task task, int[] slots) {
        if (task.duration() != null) {
            return task.duration();
        }
        if (slots == null || slots.length == 0) {
            return -1;
        }
        int length = slotEndTick[slots[0]] - slotStartTick[slots[0]];
        for (int slot : slots) {
            if (slotEndTick[slot] - slotStartTick[slot] != length) {
                return -1;
            }
        }
        return length;
    }

    /** @return availability runs [lo0,hi0,lo1,hi1,...) of a resource, or {@link #ALWAYS} when always available */
    private int[] runs(int r) {
        if (runsComputed[r]) {
            return runsCache[r];
        }
        Resource resource = resources[r];
        int[] base = null;
        if (resource != null && resource.calendarId() != null) {
            Calendar calendar = calendars.get(resource.calendarId());
            if (calendar != null) {
                List<DiagnosticIssue> ignoredWarnings = new ArrayList<>();
                base = CalendarExpander.expand(calendar, horizon, ZoneId.of(problem.timezone()), ignoredWarnings);
            }
        }
        List<int[]> blocks = new ArrayList<>();
        if (resource != null) {
            for (TimeWindow window : resource.unavailable()) {
                int lo = Math.max(0, horizon.floorTick(window.start()));
                int hi = Math.min(horizonTicks, horizon.ceilTick(window.end()));
                if (hi > lo) {
                    blocks.add(new int[]{lo, hi});
                }
            }
        }
        int[] result;
        if (base == null && blocks.isEmpty()) {
            result = ALWAYS;
        } else {
            result = subtract(base == null ? new int[]{0, horizonTicks} : base, blocks);
        }
        runsCache[r] = result;
        runsComputed[r] = true;
        return result;
    }

    /** Removes the (arbitrary, possibly overlapping) blocks from sorted disjoint runs. */
    static int[] subtract(int[] runs, List<int[]> blocks) {
        if (blocks.isEmpty()) {
            return runs;
        }
        List<int[]> sorted = new ArrayList<>(blocks);
        sorted.sort((a, b) -> Integer.compare(a[0], b[0]));
        int[] out = new int[runs.length + sorted.size() * 2];
        int n = 0;
        for (int i = 0; i + 1 < runs.length; i += 2) {
            int lo = runs[i];
            int hi = runs[i + 1];
            for (int[] block : sorted) {
                if (block[1] <= lo || block[0] >= hi) {
                    continue;
                }
                if (block[0] > lo) {
                    out = ensure(out, n + 2);
                    out[n++] = lo;
                    out[n++] = block[0];
                }
                lo = Math.max(lo, block[1]);
                if (lo >= hi) {
                    break;
                }
            }
            if (lo < hi) {
                out = ensure(out, n + 2);
                out[n++] = lo;
                out[n++] = hi;
            }
        }
        return Arrays.copyOf(out, n);
    }

    private static int[] ensure(int[] array, int size) {
        return array.length >= size ? array : Arrays.copyOf(array, Math.max(size, array.length * 2));
    }

    /** @return {@code true} when [start,end] lies inside one half-open run */
    static boolean insideOneRun(int[] runs, int start, int end) {
        int lo = 0;
        int hi = runs.length / 2 - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (runs[2 * mid] <= start) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found >= 0 && end <= runs[2 * found + 1] && (start < runs[2 * found + 1] || end == start);
    }

    private static Violation violation(String suffix, String taskId, String resourceId, String message) {
        return new Violation(PREFIX + suffix.toUpperCase(Locale.ROOT), taskId, resourceId, message);
    }
}

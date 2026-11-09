package eu.isygoit.solver.index;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.spi.PreprocessContext;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the immutable {@link CandidateIndex} of a normalized problem ONCE (design 5.4).
 * <p>
 * Pipeline (O(T + R + C + E + W), C = surviving candidates, W = calendar runs; no T x T loop, selectors evaluated once per
 * distinct selector):
 * <ol>
 *   <li>index ids, convert instants to ticks, task and requirement data, setup classes;</li>
 *   <li>expand calendars once ({@link CalendarExpander}) and derive per-resource availability profiles;</li>
 *   <li>enumerate candidates (selector bitsets, amount vs capacity, pins), guarded by {@code max-candidates};</li>
 *   <li>run the {@code preprocess} hook of every active HARD family ({@link PreprocessContext} / {@link Mutable});</li>
 *   <li>per candidate allowed start runs (window, calendar, slots, family restrictions); candidates without a feasible
 *       start are pruned (bounded record list for the infeasibility core);</li>
 *   <li>critical-path tightening of start windows over the hard precedence edges ({@link PrecedenceGraph}), re-clipping
 *       the candidate runs, then the final start domains, per-candidate overrides and slot lists;</li>
 *   <li>compaction into CSR arrays and symmetry classes ({@link SymmetryDetector}).</li>
 * </ol>
 * Structural infeasibility (a task without candidates, {@code estStart > lstStart}) is RECORDED in the index, not thrown:
 * the pre-solve analyzer reports it. The model cost estimate against the limits is done afterwards by
 * {@code ModelCostEstimator} (it also needs the objectives, which the builder does not receive).
 * <p>
 * Cancellation and the time budget ({@code options.maxTimeSeconds}) are checked every 1024 iterations through
 * {@link SolveCancelledException#check}. The builder is stateless: every call uses its own build state, so concurrent
 * builds are safe.
 */
public final class CandidateIndexBuilder {

    /** Maximum number of recorded pruned candidates. */
    public static final int MAX_PRUNED_RECORDS = 10_000;

    /** Candidate removed because the requirement amount exceeds the resource capacity. */
    public static final String REASON_AMOUNT = "AMOUNT_EXCEEDS_CAPACITY";
    /** Candidate removed because a (full) pin names other resources. */
    public static final String REASON_PIN = "PIN";
    /** Candidate removed because a pinned resource is not selected by the requirement. */
    public static final String REASON_PIN_NOT_ELIGIBLE = "PIN_NOT_ELIGIBLE";
    /** Candidate removed because no start fits between release, deadline and pin. */
    public static final String REASON_WINDOW = "WINDOW";
    /** Candidate removed because no working window of the resource fits the task. */
    public static final String REASON_CALENDAR = "CALENDAR";
    /** Candidate removed because no allowed slot start remains. */
    public static final String REASON_SLOT = "SLOT";
    /** Candidate removed because a HARD family restricted the start domain to nothing. */
    public static final String REASON_CONSTRAINT = "CONSTRAINT";
    /** Candidate removed because the precedence (CPM) window leaves no start. */
    public static final String REASON_PRECEDENCE = "PRECEDENCE_WINDOW";

    private static final int[] EMPTY = new int[0];

    private CandidateIndexBuilder() {
    }

    /**
     * Read/prune/restrict access to the candidates while HARD families run {@code preprocess} (design 5.2). Candidate
     * ids are the ENUMERATION ids of the build (request order: task, requirement, resource); they are compacted when
     * the build ends. Requirement rewriting is not offered here: it is done on the tasks by {@code TaskRewriter}
     * families in the normalizer. Times are ticks; run lists are INCLUSIVE {@code [lo0,hi0,lo1,hi1,...]}.
     */
    public interface Mutable {

        /** @return the problem being indexed */
        PlanningProblem problem();

        /** @return number of tasks */
        int taskCount();

        /** @return number of resources */
        int resourceCount();

        /** @return the resource index of an id, or {@code -1} */
        int resourceIdx(String id);

        /** @return number of enumerated candidates (removed ones included) */
        int candidateCount();

        /** @return first candidate id of the task (range {@code [candidateBegin, candidateEnd)}) */
        int candidateBegin(int task);

        /** @return one past the last candidate id of the task */
        int candidateEnd(int task);

        /** @return the task index of a candidate */
        int candidateTask(int candidate);

        /** @return the global requirement index of a candidate */
        int candidateRequirement(int candidate);

        /** @return the role name of the requirement the candidate belongs to */
        String candidateRole(int candidate);

        /** @return the resource index of a candidate */
        int candidateResource(int candidate);

        /** @return the duration in ticks of the candidate */
        int candidateDuration(int candidate);

        /** @return the capacity amount consumed by the candidate */
        int candidateAmount(int candidate);

        /** @return {@code true} when the candidate has already been removed */
        boolean removed(int candidate);

        /** Removes the candidate (idempotent) and records the reason. */
        void remove(int candidate, String reasonCode, String constraintId);

        /** Intersects the START domain of the candidate with the inclusive tick range {@code [lo, hi]}. */
        void restrictCandidateStart(int candidate, int lo, int hi);

        /** Intersects the START domain of the candidate with inclusive runs ({@code [lo0,hi0,lo1,hi1,...]}). */
        void restrictCandidateStart(int candidate, int[] runs);
    }

    /**
     * A candidate removed during the build.
     *
     * @param task         task index
     * @param requirement  global requirement index
     * @param resource     resource index
     * @param reasonCode   one of the {@code REASON_*} constants or a family reason code
     * @param constraintId id of the constraint responsible, empty for structural reasons
     */
    public record PrunedCandidate(int task, int requirement, int resource, String reasonCode, String constraintId) {
    }

    /**
     * Builds the index.
     *
     * @param problem     the normalized problem (defaults materialised; a raw problem works too)
     * @param constraints the compiled constraints; only active HARD ones run {@code preprocess}
     * @param limits      configured limits ({@code max-candidates}, {@code max-precedences}, {@code max-calendar-runs}, ...)
     * @param token       cancellation token ({@code null} = never cancelled)
     * @return the index
     * @throws LimitExceededException   when a size limit is crossed while building
     * @throws InvalidRequestException  with code {@code PRECEDENCE_CYCLE} when the edges are cyclic
     * @throws SolveCancelledException  when stop was requested or the time budget is exhausted
     */
    public static CandidateIndex build(PlanningProblem problem, List<CompiledConstraint> constraints,
                                       PlanLimitsProperties limits, SolverCancellationToken token) {
        return new Build(problem,
                constraints == null ? List.of() : constraints,
                limits == null ? new PlanLimitsProperties() : limits,
                token == null ? new SolverCancellationToken() : token).run();
    }

    // ==================================================================================================
    // Build state
    // ==================================================================================================

    private record Profile(int calendar, List<TimeWindow> unavailable) {
    }

    private static final class Build implements Mutable, PreprocessContext {

        private final PlanningProblem p;
        private final List<CompiledConstraint> constraints;
        private final PlanLimitsProperties limits;
        private final SolverCancellationToken token;
        private final long startNanos = System.nanoTime();
        private final long budgetSeconds;
        private final Horizon horizon;
        private final int h;
        private final List<Task> tasks;
        private final List<Resource> resources;
        private final List<Slot> slots;
        private final int taskCount;
        private final int resourceCount;
        private final int slotCount;
        private final CandidateIndexData d = new CandidateIndexData();
        private final ScopeResolver scopes;
        private final List<DiagnosticIssue> warnings = new ArrayList<>();
        private final List<PrunedCandidate> pruned = new ArrayList<>();
        private long prunedTotal;
        private int counter;

        // requirements
        private ResourceSelector[] reqSelector;
        private String[] roleNames;
        // resources
        private int[] resClass;
        private int[][] classRuns;
        // slots
        private int[] slotsByStart;
        private final Map<String, Integer> slotIndex = new HashMap<>();
        private int[][] slotLists;
        // raw candidates
        private IntList rawTask;
        private IntList rawReq;
        private IntList rawRes;
        private IntList rawDur;
        private IntList rawForced;
        private int[] rawTaskStart;
        private BitSet removed;
        private int[][] candRestrict;
        private int[][] taskRestrict;
        private String currentConstraintId = "";

        Build(PlanningProblem problem, List<CompiledConstraint> constraints, PlanLimitsProperties limits,
              SolverCancellationToken token) {
            this.p = problem;
            this.constraints = constraints;
            this.limits = limits;
            this.token = token;
            this.horizon = problem.horizon();
            this.h = (int) horizon.horizonTicks();
            this.tasks = problem.tasks();
            this.resources = problem.resources();
            this.slots = problem.slots();
            this.taskCount = tasks.size();
            this.resourceCount = resources.size();
            this.slotCount = slots.size();
            Integer seconds = problem.options().maxTimeSeconds();
            this.budgetSeconds = seconds == null ? 3600 : seconds;
            this.scopes = new ScopeResolver(tasks, resources);
        }

        // ------------------------------------------------------------------ main

        CandidateIndex run() {
            check();
            if (h > limits.getMaxHorizonTicks()) {
                throw new LimitExceededException("max-horizon-ticks", h, limits.getMaxHorizonTicks());
            }
            d.taskCount = taskCount;
            d.resourceCount = resourceCount;
            d.slotCount = slotCount;
            d.calendarCount = p.calendars().size();
            d.horizon = horizon;
            d.horizonTicks = h;
            d.limits = limits;
            d.scopes = scopes;
            buildSlotsAndIds();
            buildTaskData();
            buildSetupClasses();
            buildRequirements();
            buildResources();
            buildEdges();
            enumerateCandidates();
            runPreprocess();
            computeDomains();
            buildResourceCsr();
            SymmetryDetector.Result symmetry = SymmetryDetector.detect(p, d, constraints);
            d.taskSymmetryClass = symmetry.taskClass();
            d.resourceSymmetryClass = symmetry.resourceClass();
            d.pruned = pruned;
            d.prunedTotal = prunedTotal;
            d.warnings = warnings;
            return new CandidateIndex(d);
        }

        private void check() {
            SolveCancelledException.check(token, startNanos, budgetSeconds);
        }

        private void tick() {
            if ((++counter & 1023) == 0) {
                check();
            }
        }

        private int tickOf(java.time.Instant instant) {
            return Math.max(0, Math.min(h, horizon.floorTick(instant)));
        }

        // ------------------------------------------------------------------ 1. ids, slots, tasks

        private void buildSlotsAndIds() {
            d.taskIds = new String[taskCount];
            for (int t = 0; t < taskCount; t++) {
                d.taskIds[t] = tasks.get(t).id();
            }
            d.resourceIds = new String[resourceCount];
            for (int r = 0; r < resourceCount; r++) {
                d.resourceIds[r] = resources.get(r).id();
            }
            d.slotIds = new String[slotCount];
            d.slotStart = new int[slotCount];
            d.slotEnd = new int[slotCount];
            d.slotCost = new double[slotCount];
            long[] keys = new long[slotCount];
            for (int s = 0; s < slotCount; s++) {
                Slot slot = slots.get(s);
                d.slotIds[s] = slot.id();
                d.slotStart[s] = tickOf(slot.start());
                d.slotEnd[s] = tickOf(slot.end());
                d.slotCost[s] = slot.cost();
                if (slot.id() != null) {
                    slotIndex.putIfAbsent(slot.id(), s);
                }
                keys[s] = (((long) d.slotStart[s]) << 32) | s;
            }
            Arrays.sort(keys);
            slotsByStart = new int[slotCount];
            for (int i = 0; i < slotCount; i++) {
                slotsByStart[i] = (int) (keys[i] & 0xFFFFFFFFL);
            }
            slotLists = new int[taskCount][];
        }

        private void buildTaskData() {
            d.duration = new int[taskCount];
            d.demand = new int[taskCount];
            d.priority = new int[taskCount];
            d.required = new boolean[taskCount];
            d.prize = new long[taskCount];
            d.release = new int[taskCount];
            d.latestEnd = new int[taskCount];
            d.due = new int[taskCount];
            d.pinStart = new int[taskCount];
            for (int t = 0; t < taskCount; t++) {
                tick();
                Task task = tasks.get(t);
                d.demand[t] = task.demand() == null ? 1 : task.demand();
                d.priority[t] = task.priority() == null ? 0 : task.priority();
                d.required[t] = task.requiredTask();
                d.prize[t] = d.required[t] ? 0L : task.effectivePrize();
                d.release[t] = task.release() == null ? 0 : tickOf(task.release());
                d.latestEnd[t] = task.deadline() == null ? h : tickOf(task.deadline());
                d.due[t] = task.dueDate() == null ? Integer.MIN_VALUE : tickOf(task.dueDate());
                d.pinStart[t] = task.pin() == null || task.pin().start() == null ? -1 : tickOf(task.pin().start());
                d.duration[t] = task.duration() != null ? task.duration() : slotDefaultDuration(t);
            }
        }

        private int slotDefaultDuration(int t) {
            int[] list = slotList(t);
            if (list.length == 0) {
                return 0;
            }
            return d.slotEnd[list[0]] - d.slotStart[list[0]];
        }

        /** Slot indices of a slot-mode task sorted by (start, index); empty for a free-start task. */
        private int[] slotList(int t) {
            int[] cached = slotLists[t];
            if (cached != null) {
                return cached;
            }
            List<String> listed = tasks.get(t).startSlots();
            int[] result;
            if (listed.isEmpty()) {
                result = EMPTY;
            } else if (listed.stream().anyMatch("ALL"::equalsIgnoreCase)) {
                result = slotsByStart;
            } else {
                long[] keys = new long[listed.size()];
                int n = 0;
                for (String id : listed) {
                    Integer s = slotIndex.get(id);
                    if (s != null) {
                        keys[n++] = (((long) d.slotStart[s]) << 32) | s;
                    }
                }
                Arrays.sort(keys, 0, n);
                result = new int[n];
                int m = 0;
                for (int i = 0; i < n; i++) {
                    int s = (int) (keys[i] & 0xFFFFFFFFL);
                    if (m == 0 || result[m - 1] != s) {
                        result[m++] = s;
                    }
                }
                result = m == n ? result : Arrays.copyOf(result, m);
            }
            slotLists[t] = result;
            return result;
        }

        private void buildSetupClasses() {
            Map<String, Integer> classes = new HashMap<>();
            List<String> names = new ArrayList<>();
            d.setupClass = new int[taskCount];
            for (int t = 0; t < taskCount; t++) {
                String cls = tasks.get(t).setupClass();
                d.setupClass[t] = cls == null ? -1 : classIndex(classes, names, cls);
            }
            for (SetupMatrix matrix : p.setupMatrices()) {
                for (SetupEntry entry : matrix.entries()) {
                    classIndex(classes, names, entry.from());
                    classIndex(classes, names, entry.to());
                }
            }
            d.setupClassNames = names.toArray(new String[0]);
        }

        private static int classIndex(Map<String, Integer> classes, List<String> names, String name) {
            Integer idx = classes.get(name);
            if (idx == null) {
                idx = names.size();
                classes.put(name, idx);
                names.add(name);
            }
            return idx;
        }

        // ------------------------------------------------------------------ 2. requirements, resources

        private void buildRequirements() {
            IntList count = new IntList();
            IntList amount = new IntList();
            IntList role = new IntList();
            IntList owner = new IntList();
            List<ResourceSelector> selectors = new ArrayList<>();
            Map<String, Integer> roleIdx = new HashMap<>();
            List<String> roleList = new ArrayList<>();
            d.taskReqStart = new int[taskCount + 1];
            for (int t = 0; t < taskCount; t++) {
                tick();
                d.taskReqStart[t] = count.size();
                List<ResourceRequirement> reqs = tasks.get(t).effectiveRequirements();
                if (reqs.size() > limits.getMaxRequirementsPerTask()) {
                    throw new LimitExceededException("max-requirements-per-task", reqs.size(),
                            limits.getMaxRequirementsPerTask());
                }
                for (ResourceRequirement req : reqs) {
                    count.add(req.count() == null ? 1 : req.count());
                    amount.add(req.amount() == null ? d.demand[t] : req.amount());
                    Integer r = roleIdx.get(req.role());
                    if (r == null) {
                        r = roleList.size();
                        roleIdx.put(req.role(), r);
                        roleList.add(req.role());
                    }
                    role.add(r);
                    owner.add(t);
                    selectors.add(req.selector());
                }
            }
            d.taskReqStart[taskCount] = count.size();
            d.requirementCount = count.size();
            d.reqCount = count.toArray();
            d.reqAmount = amount.toArray();
            d.reqRole = role.toArray();
            d.reqTask = owner.toArray();
            d.roles = roleList.toArray(new String[0]);
            reqSelector = selectors.toArray(new ResourceSelector[0]);
            roleNames = d.roles;
        }

        private void buildResources() {
            d.capacity = new int[resourceCount];
            d.resCalendar = new int[resourceCount];
            d.resSetupMatrix = new int[resourceCount];
            d.maxLoadTicks = new int[resourceCount];
            d.resNeverAvailable = new boolean[resourceCount];
            Map<String, Integer> calendarIdx = new HashMap<>();
            List<Calendar> calendars = p.calendars();
            int[][] calendarRuns = new int[calendars.size()][];
            long totalRuns = 0;
            ZoneId zone = ZoneId.of(p.timezone());
            for (int c = 0; c < calendars.size(); c++) {
                calendarIdx.putIfAbsent(calendars.get(c).id(), c);
                List<DiagnosticIssue> local = new ArrayList<>();
                calendarRuns[c] = CalendarExpander.expand(calendars.get(c), horizon, zone, local,
                        limits.getMaxCalendarRuns());
                for (DiagnosticIssue issue : local) {
                    warnings.add(new DiagnosticIssue(issue.code(), issue.severity(),
                            "calendars[" + c + "]." + issue.path(), issue.message()));
                }
                totalRuns += calendarRuns[c].length / 2;
                if (totalRuns > limits.getMaxCalendarRuns()) {
                    throw new LimitExceededException("max-calendar-runs", totalRuns, limits.getMaxCalendarRuns());
                }
                check();
            }
            Map<String, Integer> matrixIdx = new HashMap<>();
            for (int m = 0; m < p.setupMatrices().size(); m++) {
                matrixIdx.putIfAbsent(p.setupMatrices().get(m).id(), m);
            }
            Map<Profile, Integer> profiles = new HashMap<>();
            List<int[]> runsOfClass = new ArrayList<>();
            resClass = new int[resourceCount];
            IntList runStart = new IntList(resourceCount + 1);
            IntList runs = new IntList();
            for (int r = 0; r < resourceCount; r++) {
                tick();
                Resource resource = resources.get(r);
                d.capacity[r] = resource.capacity() == null ? 1 : resource.capacity();
                d.maxLoadTicks[r] = resource.maxLoadTicks() == null ? Integer.MAX_VALUE : resource.maxLoadTicks();
                Integer matrix = resource.setupMatrixId() == null ? null : matrixIdx.get(resource.setupMatrixId());
                d.resSetupMatrix[r] = matrix == null ? -1 : matrix;
                Integer calendar = resource.calendarId() == null ? null : calendarIdx.get(resource.calendarId());
                d.resCalendar[r] = calendar == null ? -1 : calendar;
                runStart.add(runs.size());
                if (calendar == null && resource.unavailable().isEmpty()) {
                    resClass[r] = -1;
                    continue;
                }
                Profile profile = new Profile(d.resCalendar[r], resource.unavailable());
                Integer cls = profiles.get(profile);
                if (cls == null) {
                    int[] base = calendar == null ? new int[]{0, h} : calendarRuns[calendar];
                    List<DiagnosticIssue> local = new ArrayList<>();
                    int[] blocks = CalendarExpander.blockedRuns(resource.unavailable(), horizon, local,
                            "resources[" + r + "].unavailable");
                    warnings.addAll(local);
                    cls = runsOfClass.size();
                    profiles.put(profile, cls);
                    runsOfClass.add(CalendarExpander.subtract(base, blocks));
                }
                resClass[r] = cls;
                int[] resourceRuns = runsOfClass.get(cls);
                d.resNeverAvailable[r] = resourceRuns.length == 0;
                for (int v : resourceRuns) {
                    runs.add(v);
                }
            }
            runStart.add(runs.size());
            d.resRunStart = runStart.toArray();
            d.resRuns = runs.toArray();
            classRuns = runsOfClass.toArray(new int[0][]);
        }

        // ------------------------------------------------------------------ 3. precedence edges

        private void buildEdges() {
            IntList pred = new IntList();
            IntList succ = new IntList();
            IntList type = new IntList();
            IntList minLag = new IntList();
            IntList maxLag = new IntList();
            IntList implies = new IntList();
            d.succEdgeStart = new int[taskCount + 1];
            for (int t = 0; t < taskCount; t++) {
                tick();
                d.succEdgeStart[t] = pred.size();
                for (PrecedenceRef ref : tasks.get(t).predecessors()) {
                    int from = scopes.taskIdx(ref.taskId());
                    if (from < 0 || from == t) {
                        continue;
                    }
                    pred.add(from);
                    succ.add(t);
                    type.add(ref.type().ordinal());
                    minLag.add(ref.minLag() == null ? 0 : ref.minLag());
                    maxLag.add(ref.maxLag() == null ? Integer.MAX_VALUE : ref.maxLag());
                    implies.add(ref.presence() == PrecedenceRef.Presence.IMPLIES ? 1 : 0);
                }
            }
            d.succEdgeStart[taskCount] = pred.size();
            if (pred.size() > limits.getMaxPrecedences()) {
                throw new LimitExceededException("max-precedences", pred.size(), limits.getMaxPrecedences());
            }
            d.edgeCount = pred.size();
            d.edgePred = pred.toArray();
            d.edgeSucc = succ.toArray();
            d.edgeType = type.toArray();
            d.edgeMinLag = minLag.toArray();
            d.edgeMaxLag = maxLag.toArray();
            d.edgeImpliesPresence = implies.toArray();
            int[] topo = PrecedenceGraph.topologicalOrder(taskCount, d.edgePred, d.edgeSucc);
            if (topo == null) {
                List<String> cycle = PrecedenceGraph.findCycle(tasks);
                String path = cycle.isEmpty() ? "" : String.join(" -> ", cycle) + " -> " + cycle.get(0);
                throw new InvalidRequestException("PRECEDENCE_CYCLE", "Precedence cycle: " + path);
            }
            d.topoOrder = topo;
        }

        // ------------------------------------------------------------------ 4. candidates

        private void enumerateCandidates() {
            SelectorEvaluator selectors = new SelectorEvaluator(resources);
            long bound = 0;
            long failFast = limits.getMaxCandidates() * 4;
            for (int q = 0; q < reqSelector.length; q++) {
                bound += selectors.cardinality(reqSelector[q]);
                if (bound > failFast) {
                    throw new LimitExceededException("max-candidates", bound, failFast);
                }
            }
            rawTask = new IntList();
            rawReq = new IntList();
            rawRes = new IntList();
            rawDur = new IntList();
            rawForced = new IntList();
            rawTaskStart = new int[taskCount + 1];
            for (int t = 0; t < taskCount; t++) {
                check();
                rawTaskStart[t] = rawTask.size();
                Task task = tasks.get(t);
                int qBegin = d.taskReqStart[t];
                int qEnd = d.taskReqStart[t + 1];
                int[][] pins = assignPins(task, qBegin, qEnd);
                Map<Integer, Integer> durationByResource = resourceDurations(task, qBegin, qEnd);
                for (int q = qBegin; q < qEnd; q++) {
                    int[] pinned = pins == null ? null : pins[q - qBegin];
                    boolean fullPin = pinned != null && pinned.length >= d.reqCount[q];
                    int[] selected = selectors.resources(reqSelector[q]);
                    if (pinned != null) {
                        for (int r : pinned) {
                            if (Arrays.binarySearch(selected, r) < 0) {
                                record(t, q, r, REASON_PIN_NOT_ELIGIBLE, "");
                            }
                        }
                    }
                    for (int r : selected) {
                        tick();
                        if (d.reqAmount[q] > d.capacity[r]) {
                            record(t, q, r, REASON_AMOUNT, "");
                            continue;
                        }
                        boolean isPinned = pinned != null && contains(pinned, r);
                        if (fullPin && !isPinned) {
                            record(t, q, r, REASON_PIN, "");
                            continue;
                        }
                        Integer own = durationByResource == null ? null : durationByResource.get(r);
                        rawTask.add(t);
                        rawReq.add(q);
                        rawRes.add(r);
                        rawDur.add(own == null ? d.duration[t] : own);
                        rawForced.add(isPinned && !fullPin ? 1 : 0);
                        if (rawTask.size() > limits.getMaxCandidates()) {
                            throw new LimitExceededException("max-candidates", rawTask.size(),
                                    limits.getMaxCandidates());
                        }
                    }
                }
            }
            rawTaskStart[taskCount] = rawTask.size();
            removed = new BitSet(rawTask.size());
            candRestrict = new int[rawTask.size()][];
            taskRestrict = new int[taskCount][];
        }

        private int[][] assignPins(Task task, int qBegin, int qEnd) {
            if (task.pin() == null || task.pin().resourceIds().isEmpty()) {
                return null;
            }
            IntList ids = new IntList();
            for (String id : task.pin().resourceIds()) {
                int r = scopes.resourceIdx(id);
                if (r >= 0) {
                    ids.add(r);
                }
            }
            int[][] result = new int[qEnd - qBegin][];
            int next = 0;
            for (int q = qBegin; q < qEnd && next < ids.size(); q++) {
                int take = Math.min(d.reqCount[q], ids.size() - next);
                int[] part = new int[take];
                for (int i = 0; i < take; i++) {
                    part[i] = ids.get(next++);
                }
                result[q - qBegin] = part;
            }
            return result;
        }

        /** {@code resourceDurations} apply only to a task with exactly one requirement of count 1. */
        private Map<Integer, Integer> resourceDurations(Task task, int qBegin, int qEnd) {
            if (task.resourceDurations().isEmpty() || qEnd - qBegin != 1 || d.reqCount[qBegin] != 1) {
                return null;
            }
            Map<Integer, Integer> map = new HashMap<>();
            for (Map.Entry<String, Integer> e : task.resourceDurations().entrySet()) {
                int r = scopes.resourceIdx(e.getKey());
                if (r >= 0 && e.getValue() != null) {
                    map.put(r, e.getValue());
                }
            }
            return map;
        }

        private static boolean contains(int[] values, int v) {
            for (int x : values) {
                if (x == v) {
                    return true;
                }
            }
            return false;
        }

        private void record(int task, int requirement, int resource, String reason, String constraintId) {
            prunedTotal++;
            if (pruned.size() < MAX_PRUNED_RECORDS) {
                pruned.add(new PrunedCandidate(task, requirement, resource, reason, constraintId));
            }
        }

        private void runPreprocess() {
            for (CompiledConstraint cc : constraints) {
                check();
                IEnumEnforcement.Types enforcement = cc.constraint().enforcement();
                boolean hard = enforcement == IEnumEnforcement.Types.STRICTLY_REQUIRED
                        || enforcement == IEnumEnforcement.Types.CONFIGURABLE_HARD;
                if (cc.active() && hard && cc.family() != null) {
                    currentConstraintId = cc.constraint().id();
                    cc.family().preprocess(this, cc.constraint(), cc.params());
                    currentConstraintId = "";
                }
            }
        }

        // ------------------------------------------------------------------ 5. start domains

        private void computeDomains() {
            int rawCount = rawTask.size();
            StartDomainCalculator calc = new StartDomainCalculator(classRuns);
            int[][] allowed = new int[rawCount][];
            int[] est = new int[taskCount];
            int[] lst = new int[taskCount];
            int[] minDur = new int[taskCount];
            int[] maxDur = new int[taskCount];
            int[] surviving = new int[taskCount];
            for (int t = 0; t < taskCount; t++) {
                check();
                boolean slotMode = !tasks.get(t).startSlots().isEmpty();
                int[] slotList = slotMode ? slotList(t) : EMPTY;
                int pin = d.pinStart[t];
                int lo = Math.max(d.release[t], pin >= 0 ? pin : 0);
                int[] union = EMPTY;
                int[] previous = null;
                int min = Integer.MAX_VALUE;
                int max = -1;
                int alive = 0;
                for (int c = rawTaskStart[t]; c < rawTaskStart[t + 1]; c++) {
                    tick();
                    if (removed.get(c)) {
                        continue;
                    }
                    int dur = rawDur.get(c);
                    int r = rawRes.get(c);
                    if (d.resNeverAvailable[r]) {
                        remove(c, REASON_CALENDAR, "");
                        continue;
                    }
                    int hi = d.latestEnd[t] - dur;
                    if (pin >= 0) {
                        hi = Math.min(hi, pin);
                    }
                    if (hi < lo) {
                        remove(c, REASON_WINDOW, "");
                        continue;
                    }
                    int[] runs = calc.allowed(resClass[r], dur, lo, hi);
                    if (runs.length == 0) {
                        remove(c, REASON_CALENDAR, "");
                        continue;
                    }
                    if (taskRestrict[t] != null) {
                        runs = StartDomainCalculator.intersect(runs, taskRestrict[t]);
                    }
                    if (candRestrict[c] != null && runs.length > 0) {
                        runs = StartDomainCalculator.intersect(runs, candRestrict[c]);
                    }
                    if (runs.length == 0) {
                        remove(c, REASON_CONSTRAINT, "");
                        continue;
                    }
                    if (slotMode) {
                        runs = slotStarts(runs, slotList, dur);
                        if (runs.length == 0) {
                            remove(c, REASON_SLOT, "");
                            continue;
                        }
                    }
                    allowed[c] = runs;
                    if (runs != previous) {
                        union = StartDomainCalculator.union(union, runs);
                        previous = runs;
                    }
                    min = Math.min(min, dur);
                    max = Math.max(max, dur);
                    alive++;
                }
                surviving[t] = alive;
                if (alive > 0) {
                    est[t] = StartDomainCalculator.min(union);
                    lst[t] = StartDomainCalculator.max(union);
                    minDur[t] = min;
                    maxDur[t] = max;
                } else {
                    est[t] = lo;
                    lst[t] = Math.min(d.latestEnd[t] - d.duration[t], pin >= 0 ? pin : Integer.MAX_VALUE);
                    minDur[t] = d.duration[t];
                    maxDur[t] = d.duration[t];
                }
            }
            check();
            PrecedenceGraph.propagateWindows(est, lst, minDur, maxDur, d.required, d.succEdgeStart, d.edgePred,
                    d.edgeType, d.edgeMinLag, d.topoOrder);
            // re-clip the candidates to the tightened windows, final unions, slot survivors
            int[][] union = new int[taskCount][];
            int[] stamp = new int[slotCount];
            IntList taskSlotStart = new IntList(taskCount + 1);
            IntList taskSlots = new IntList();
            IntList taskRunStart = new IntList(taskCount + 1);
            IntList taskRuns = new IntList();
            for (int t = 0; t < taskCount; t++) {
                check();
                int[] merged = EMPTY;
                int[] previous = null;
                int alive = 0;
                for (int c = rawTaskStart[t]; c < rawTaskStart[t + 1]; c++) {
                    tick();
                    if (removed.get(c)) {
                        continue;
                    }
                    int[] runs = StartDomainCalculator.clip(allowed[c], est[t], lst[t]);
                    if (runs.length == 0) {
                        remove(c, REASON_PRECEDENCE, "");
                        continue;
                    }
                    allowed[c] = runs;
                    if (runs != previous) {
                        merged = StartDomainCalculator.union(merged, runs);
                        previous = runs;
                    }
                    alive++;
                }
                union[t] = merged;
                if (alive > 0) {
                    est[t] = StartDomainCalculator.min(merged);
                    lst[t] = StartDomainCalculator.max(merged);
                }
                surviving[t] = alive;
                taskSlotStart.add(taskSlots.size());
                if (!tasks.get(t).startSlots().isEmpty() && alive > 0) {
                    int[] slotList = slotList(t);
                    for (int c = rawTaskStart[t]; c < rawTaskStart[t + 1]; c++) {
                        if (removed.get(c)) {
                            continue;
                        }
                        int dur = rawDur.get(c);
                        for (int s : slotList) {
                            if (d.slotEnd[s] - d.slotStart[s] >= dur
                                    && StartDomainCalculator.contains(allowed[c], d.slotStart[s])) {
                                stamp[s] = t + 1;
                            }
                        }
                    }
                    for (int s : slotList) {
                        if (stamp[s] == t + 1) {
                            taskSlots.add(s);
                        }
                    }
                }
                taskRunStart.add(taskRuns.size());
                if (merged.length > 2) {
                    for (int v : merged) {
                        taskRuns.add(v);
                    }
                }
            }
            taskSlotStart.add(taskSlots.size());
            taskRunStart.add(taskRuns.size());
            d.estStart = est;
            d.lstStart = lst;
            d.taskSlotStart = taskSlotStart.toArray();
            d.taskSlots = taskSlots.toArray();
            d.taskStartRunStart = taskRunStart.toArray();
            d.taskStartRuns = taskRuns.toArray();
            compact(allowed, union);
        }

        /** Restricts runs to the starts of the task's slots that are long enough, as merged point runs. */
        private int[] slotStarts(int[] runs, int[] slotList, int dur) {
            IntList out = new IntList();
            int lastEnd = Integer.MIN_VALUE;
            for (int s : slotList) {
                int start = d.slotStart[s];
                if (d.slotEnd[s] - start < dur || start == lastEnd || !StartDomainCalculator.contains(runs, start)) {
                    continue;
                }
                if (out.size() > 0 && out.get(out.size() - 1) + 1 == start) {
                    out.set(out.size() - 1, start);
                } else {
                    out.add(start);
                    out.add(start);
                }
                lastEnd = start;
            }
            return out.toArray();
        }

        /** Drops removed candidates, renumbers, emits per-candidate overrides and the requirement CSR. */
        private void compact(int[][] allowed, int[][] union) {
            int rawCount = rawTask.size();
            IntList candTask = new IntList(rawCount);
            IntList candReq = new IntList(rawCount);
            IntList candRes = new IntList(rawCount);
            IntList candDur = new IntList(rawCount);
            IntList candAmount = new IntList(rawCount);
            IntList candForced = new IntList(rawCount);
            IntList overrideStart = new IntList(rawCount + 1);
            IntList overrides = new IntList();
            int[] kept = new int[d.requirementCount + 1];
            d.taskCandStart = new int[taskCount + 1];
            for (int t = 0; t < taskCount; t++) {
                check();
                d.taskCandStart[t] = candTask.size();
                for (int c = rawTaskStart[t]; c < rawTaskStart[t + 1]; c++) {
                    if (removed.get(c)) {
                        continue;
                    }
                    int q = rawReq.get(c);
                    candTask.add(t);
                    candReq.add(q);
                    candRes.add(rawRes.get(c));
                    candDur.add(rawDur.get(c));
                    candAmount.add(d.reqAmount[q]);
                    candForced.add(rawForced.get(c));
                    kept[q + 1]++;
                    overrideStart.add(overrides.size());
                    if (!Arrays.equals(allowed[c], union[t])) {
                        for (int v : allowed[c]) {
                            overrides.add(v);
                        }
                    }
                }
            }
            d.taskCandStart[taskCount] = candTask.size();
            overrideStart.add(overrides.size());
            for (int q = 0; q < d.requirementCount; q++) {
                kept[q + 1] += kept[q];
            }
            d.reqCandStart = kept;
            d.candidateCount = candTask.size();
            d.candTask = candTask.toArray();
            d.candReq = candReq.toArray();
            d.candResource = candRes.toArray();
            d.candDuration = candDur.toArray();
            d.candAmount = candAmount.toArray();
            d.candStartRunStart = overrideStart.toArray();
            d.candStartRuns = overrides.toArray();
            d.candForced = new boolean[d.candidateCount];
            for (int c = 0; c < d.candidateCount; c++) {
                d.candForced[c] = candForced.get(c) == 1;
            }
        }

        private void buildResourceCsr() {
            int n = d.candidateCount;
            d.resCandStart = new int[resourceCount + 1];
            for (int c = 0; c < n; c++) {
                d.resCandStart[d.candResource[c] + 1]++;
            }
            for (int r = 0; r < resourceCount; r++) {
                d.resCandStart[r + 1] += d.resCandStart[r];
            }
            int[] fill = new int[resourceCount];
            d.resCand = new int[n];
            for (int c = 0; c < n; c++) {
                int r = d.candResource[c];
                d.resCand[d.resCandStart[r] + fill[r]++] = c;
            }
        }

        // ------------------------------------------------------------------ Mutable / PreprocessContext

        @Override
        public PlanningProblem problem() {
            return p;
        }

        @Override
        public Mutable candidates() {
            return this;
        }

        @Override
        public int[] resolveTasks(ConstraintScope scope) {
            return scopes.resolveTasks(scope);
        }

        @Override
        public int[] resolveResources(ConstraintScope scope) {
            return scopes.resolveResources(scope);
        }

        @Override
        public Horizon horizon() {
            return horizon;
        }

        @Override
        public void pruneCandidate(int candidate, String reasonCode, String constraintId) {
            remove(candidate, reasonCode, constraintId);
        }

        @Override
        public void restrictStart(int task, int lo, int hi) {
            restrictStartDomain(task, hi < lo ? EMPTY : new int[]{lo, hi});
        }

        @Override
        public void restrictStartDomain(int task, int[] runs) {
            taskRestrict[task] = taskRestrict[task] == null
                    ? runs.clone() : StartDomainCalculator.intersect(taskRestrict[task], runs);
        }

        @Override
        public int taskCount() {
            return taskCount;
        }

        @Override
        public int resourceCount() {
            return resourceCount;
        }

        @Override
        public int resourceIdx(String id) {
            return scopes.resourceIdx(id);
        }

        @Override
        public int candidateCount() {
            return rawTask.size();
        }

        @Override
        public int candidateBegin(int task) {
            return rawTaskStart[task];
        }

        @Override
        public int candidateEnd(int task) {
            return rawTaskStart[task + 1];
        }

        @Override
        public int candidateTask(int candidate) {
            return rawTask.get(candidate);
        }

        @Override
        public int candidateRequirement(int candidate) {
            return rawReq.get(candidate);
        }

        @Override
        public String candidateRole(int candidate) {
            return roleNames[d.reqRole[rawReq.get(candidate)]];
        }

        @Override
        public int candidateResource(int candidate) {
            return rawRes.get(candidate);
        }

        @Override
        public int candidateDuration(int candidate) {
            return rawDur.get(candidate);
        }

        @Override
        public int candidateAmount(int candidate) {
            return d.reqAmount[rawReq.get(candidate)];
        }

        @Override
        public boolean removed(int candidate) {
            return removed.get(candidate);
        }

        @Override
        public void remove(int candidate, String reasonCode, String constraintId) {
            if (!removed.get(candidate)) {
                removed.set(candidate);
                record(rawTask.get(candidate), rawReq.get(candidate), rawRes.get(candidate), reasonCode,
                        constraintId == null || constraintId.isEmpty() ? currentConstraintId : constraintId);
            }
        }

        @Override
        public void restrictCandidateStart(int candidate, int lo, int hi) {
            restrictCandidateStart(candidate, hi < lo ? EMPTY : new int[]{lo, hi});
        }

        @Override
        public void restrictCandidateStart(int candidate, int[] runs) {
            candRestrict[candidate] = candRestrict[candidate] == null
                    ? runs.clone() : StartDomainCalculator.intersect(candRestrict[candidate], runs);
        }
    }
}

package eu.isygoit.solver.index;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.solver.spi.CandidateIndexView;

import java.util.List;

/**
 * The immutable, int-indexed model of a normalized problem, built ONCE by {@link CandidateIndexBuilder} (design 5.4).
 * <p>
 * Indices are request order; all times are integer ticks (tick 0 = horizon start); the arrays are shared with engines
 * and families: callers MUST NOT modify them. Strings appear only in {@link #taskIds}, {@link #resourceIds},
 * {@link #slotIds}, {@link #roles} and the build-time lookup maps ({@link #taskIdx(String)} and friends, never for hot
 * loops). Thread-safe after construction (the scope caches are concurrent).
 * <p>
 * Conventions: CSR ranges are {@code [start[i], start[i+1])}; candidates of a task are sorted by requirement, then
 * resource index; {@code Integer.MAX_VALUE} means "no limit" (maxLag, maxLoadTicks), {@code Integer.MIN_VALUE} "no due
 * date", {@code -1} "none" (pin, class, calendar, matrix). An EMPTY run range means "no restriction" for
 * {@link #resRunStart} (always available), {@link #candStartRunStart} (same as the task domain) and
 * {@link #taskSlotStart} (free start). A task whose {@code estStart > lstStart} (or without candidates) is structurally
 * infeasible; the builder records it, it does not throw (the pre-solve analyzer reports it).
 * <p>
 * Additions to the design's field list (all documented here): {@link #taskStartRunStart}/{@link #taskStartRuns} (the task
 * start domain with holes, inclusive runs), {@link #reqTask}, {@link #reqCandStart}, {@link #candForced},
 * {@link #resNeverAvailable}, {@link #setupClassNames}, {@link #prunedCandidates}.
 */
public final class CandidateIndex implements CandidateIndexView {

    // ---- sizes
    public final int taskCount, resourceCount, slotCount, calendarCount, candidateCount, requirementCount, edgeCount;
    public final Horizon horizon;
    public final int horizonTicks;
    public final PlanLimitsProperties limits;
    // ---- ids
    public final String[] taskIds, resourceIds, slotIds, roles;
    /** Setup class names (global dictionary): {@link #setupClass} indexes it. */
    public final String[] setupClassNames;
    // ---- tasks
    public final int[] duration, demand, priority;
    public final boolean[] required;
    public final long[] prize;
    public final int[] release, latestEnd, due;
    public final int[] estStart, lstStart;
    public final int[] pinStart;
    public final int[] setupClass;
    /**
     * CSR by task of the union start domain as INCLUSIVE runs {@code [lo0,hi0,lo1,hi1,...]} (calendar holes, slot
     * starts, family restrictions). Empty range = the plain interval {@code [estStart, lstStart]}.
     */
    public final int[] taskStartRunStart, taskStartRuns;
    // ---- requirements (CSR by task)
    public final int[] taskReqStart;
    public final int[] reqCount, reqAmount, reqRole;
    /** Owning task of each requirement. */
    public final int[] reqTask;
    /** CSR by requirement into the candidates ({@code [reqCandStart[q], reqCandStart[q+1])}, {@code Q + 1} entries). */
    public final int[] reqCandStart;
    // ---- candidates (CSR by task)
    public final int[] taskCandStart;
    public final int[] candTask, candReq, candResource, candDuration, candAmount;
    /** {@code true} when the candidate resource was named by a PARTIAL {@code Task.pin.resourceIds}: it must be used. */
    public final boolean[] candForced;
    public final int[] candStartRunStart, candStartRuns;
    // ---- candidates (CSR by resource)
    public final int[] resCandStart;
    public final int[] resCand;
    // ---- resources
    public final int[] capacity;
    public final int[] resCalendar;
    public final int[] resRunStart, resRuns;
    /** {@code true} when the resource has no availability at all inside the horizon (all its candidates are pruned). */
    public final boolean[] resNeverAvailable;
    public final int[] resSetupMatrix;
    public final int[] maxLoadTicks;
    // ---- precedence edges (CSR by successor)
    public final int[] succEdgeStart, edgePred, edgeSucc, edgeType, edgeMinLag, edgeMaxLag, edgeImpliesPresence;
    public final int[] topoOrder;
    // ---- slots
    public final int[] slotStart, slotEnd;
    public final double[] slotCost;
    public final int[] taskSlotStart, taskSlots;
    // ---- symmetry
    public final int[] taskSymmetryClass, resourceSymmetryClass;
    // ---- diagnostics
    /** Candidates removed during the build (bounded to 10 000 records) with the reason, for the infeasibility core. */
    public final List<CandidateIndexBuilder.PrunedCandidate> prunedCandidates;
    /** Total number of removed candidates (can exceed the size of {@link #prunedCandidates}). */
    public final long prunedTotal;
    /** Calendar warnings produced while expanding availability. */
    public final List<DiagnosticIssue> warnings;

    private final ScopeResolver scopes;

    CandidateIndex(CandidateIndexData d) {
        taskCount = d.taskCount;
        resourceCount = d.resourceCount;
        slotCount = d.slotCount;
        calendarCount = d.calendarCount;
        candidateCount = d.candidateCount;
        requirementCount = d.requirementCount;
        edgeCount = d.edgeCount;
        horizon = d.horizon;
        horizonTicks = d.horizonTicks;
        limits = d.limits;
        taskIds = d.taskIds;
        resourceIds = d.resourceIds;
        slotIds = d.slotIds;
        roles = d.roles;
        setupClassNames = d.setupClassNames;
        duration = d.duration;
        demand = d.demand;
        priority = d.priority;
        required = d.required;
        prize = d.prize;
        release = d.release;
        latestEnd = d.latestEnd;
        due = d.due;
        estStart = d.estStart;
        lstStart = d.lstStart;
        pinStart = d.pinStart;
        setupClass = d.setupClass;
        taskStartRunStart = d.taskStartRunStart;
        taskStartRuns = d.taskStartRuns;
        taskReqStart = d.taskReqStart;
        reqCount = d.reqCount;
        reqAmount = d.reqAmount;
        reqRole = d.reqRole;
        reqTask = d.reqTask;
        reqCandStart = d.reqCandStart;
        taskCandStart = d.taskCandStart;
        candTask = d.candTask;
        candReq = d.candReq;
        candResource = d.candResource;
        candDuration = d.candDuration;
        candAmount = d.candAmount;
        candForced = d.candForced;
        candStartRunStart = d.candStartRunStart;
        candStartRuns = d.candStartRuns;
        resCandStart = d.resCandStart;
        resCand = d.resCand;
        capacity = d.capacity;
        resCalendar = d.resCalendar;
        resRunStart = d.resRunStart;
        resRuns = d.resRuns;
        resNeverAvailable = d.resNeverAvailable;
        resSetupMatrix = d.resSetupMatrix;
        maxLoadTicks = d.maxLoadTicks;
        succEdgeStart = d.succEdgeStart;
        edgePred = d.edgePred;
        edgeSucc = d.edgeSucc;
        edgeType = d.edgeType;
        edgeMinLag = d.edgeMinLag;
        edgeMaxLag = d.edgeMaxLag;
        edgeImpliesPresence = d.edgeImpliesPresence;
        topoOrder = d.topoOrder;
        slotStart = d.slotStart;
        slotEnd = d.slotEnd;
        slotCost = d.slotCost;
        taskSlotStart = d.taskSlotStart;
        taskSlots = d.taskSlots;
        taskSymmetryClass = d.taskSymmetryClass;
        resourceSymmetryClass = d.resourceSymmetryClass;
        prunedCandidates = List.copyOf(d.pruned);
        prunedTotal = d.prunedTotal;
        warnings = List.copyOf(d.warnings);
        scopes = d.scopes;
    }

    // ------------------------------------------------------------------ CandidateIndexView: sizes, ids, scopes

    @Override
    public int taskCount() {
        return taskCount;
    }

    @Override
    public int resourceCount() {
        return resourceCount;
    }

    @Override
    public int slotCount() {
        return slotCount;
    }

    @Override
    public Horizon horizon() {
        return horizon;
    }

    @Override
    public int horizonTicks() {
        return horizonTicks;
    }

    @Override
    public PlanLimitsProperties limits() {
        return limits;
    }

    @Override
    public String taskId(int t) {
        return taskIds[t];
    }

    @Override
    public String resourceId(int r) {
        return resourceIds[r];
    }

    @Override
    public String slotId(int s) {
        return slotIds[s];
    }

    @Override
    public int taskIdx(String id) {
        return scopes.taskIdx(id);
    }

    @Override
    public int resourceIdx(String id) {
        return scopes.resourceIdx(id);
    }

    @Override
    public int slotIdx(String id) {
        return SlotLookup.indexOf(slotIds, id);
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
    public int[][] groupTasksByTagPrefix(String prefix) {
        return scopes.groupTasksByTagPrefix(prefix);
    }

    // ------------------------------------------------------------------ CandidateIndexView: index-only data

    @Override
    public int candidateCount() {
        return candidateCount;
    }

    @Override
    public int duration(int task) {
        return duration[task];
    }

    @Override
    public int demand(int task) {
        return demand[task];
    }

    @Override
    public int priority(int task) {
        return priority[task];
    }

    @Override
    public boolean required(int task) {
        return required[task];
    }

    @Override
    public long prize(int task) {
        return prize[task];
    }

    @Override
    public int release(int task) {
        return release[task];
    }

    @Override
    public int latestEnd(int task) {
        return latestEnd[task];
    }

    @Override
    public int due(int task) {
        return due[task];
    }

    @Override
    public int estStart(int task) {
        return estStart[task];
    }

    @Override
    public int lstStart(int task) {
        return lstStart[task];
    }

    @Override
    public int pinStart(int task) {
        return pinStart[task];
    }

    @Override
    public int capacity(int resource) {
        return capacity[resource];
    }

    @Override
    public int maxLoadTicks(int resource) {
        return maxLoadTicks[resource];
    }

    @Override
    public int candidateBegin(int task) {
        return taskCandStart[task];
    }

    @Override
    public int candidateEnd(int task) {
        return taskCandStart[task + 1];
    }

    @Override
    public int candidateTask(int candidate) {
        return candTask[candidate];
    }

    @Override
    public int candidateResource(int candidate) {
        return candResource[candidate];
    }

    @Override
    public int candidateDuration(int candidate) {
        return candDuration[candidate];
    }

    @Override
    public int candidateAmount(int candidate) {
        return candAmount[candidate];
    }

    /** Linear lookup helper (slot ids are only resolved at build/validation time). */
    private static final class SlotLookup {
        static int indexOf(String[] ids, String id) {
            if (id == null) {
                return -1;
            }
            for (int i = 0; i < ids.length; i++) {
                if (id.equals(ids[i])) {
                    return i;
                }
            }
            return -1;
        }
    }
}

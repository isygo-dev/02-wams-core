package eu.isygoit.solver.index;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.response.DiagnosticIssue;

import java.util.List;

/**
 * Mutable carrier of the arrays produced by {@link CandidateIndexBuilder}; copied once into the immutable
 * {@link CandidateIndex}. Package-private, single-threaded, never exposed.
 */
final class CandidateIndexData {

    int taskCount;
    int resourceCount;
    int slotCount;
    int calendarCount;
    int candidateCount;
    int requirementCount;
    int edgeCount;
    Horizon horizon;
    int horizonTicks;
    PlanLimitsProperties limits;
    ScopeResolver scopes;

    String[] taskIds;
    String[] resourceIds;
    String[] slotIds;
    String[] roles;
    String[] setupClassNames;

    int[] duration;
    int[] demand;
    int[] priority;
    boolean[] required;
    long[] prize;
    int[] release;
    int[] latestEnd;
    int[] due;
    int[] estStart;
    int[] lstStart;
    int[] pinStart;
    int[] setupClass;
    int[] taskStartRunStart;
    int[] taskStartRuns;

    int[] taskReqStart;
    int[] reqCount;
    int[] reqAmount;
    int[] reqRole;
    int[] reqTask;
    int[] reqCandStart;

    int[] taskCandStart;
    int[] candTask;
    int[] candReq;
    int[] candResource;
    int[] candDuration;
    int[] candAmount;
    boolean[] candForced;
    int[] candStartRunStart;
    int[] candStartRuns;
    int[] resCandStart;
    int[] resCand;

    int[] capacity;
    int[] resCalendar;
    int[] resRunStart;
    int[] resRuns;
    boolean[] resNeverAvailable;
    int[] resSetupMatrix;
    int[] maxLoadTicks;

    int[] succEdgeStart;
    int[] edgePred;
    int[] edgeSucc;
    int[] edgeType;
    int[] edgeMinLag;
    int[] edgeMaxLag;
    int[] edgeImpliesPresence;
    int[] topoOrder;

    int[] slotStart;
    int[] slotEnd;
    double[] slotCost;
    int[] taskSlotStart;
    int[] taskSlots;

    int[] taskSymmetryClass;
    int[] resourceSymmetryClass;

    List<CandidateIndexBuilder.PrunedCandidate> pruned;
    long prunedTotal;
    List<DiagnosticIssue> warnings;
}

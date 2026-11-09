package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.SolutionView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Explains why each OPTIONAL task is not part of the plan (design 8.4), in O(T + E).
 * <p>
 * Reason codes, tested in this order: {@code NO_ELIGIBLE_RESOURCE} (no candidate survived the index pruning; the
 * suggestion names the hard constraints that may have pruned it), {@code OUTSIDE_WINDOW} (empty start domain,
 * {@code estStart > lstStart}), {@code PREDECESSOR_NOT_SELECTED} (an {@code IMPLIES} predecessor is not selected), else
 * {@code NOT_SELECTED} (the optimiser preferred other tasks). {@code forgonePrize} is the effective prize
 * ({@code prize ?? 1}). The result is sorted by task id (design 8.3).
 * <p>
 * Uses the per-task data of the {@link CandidateIndexView} ({@code candidateBegin/End}, {@code estStart/lstStart}), so it
 * needs a full index. Immutable after construction; thread-safe.
 */
public final class UnselectedReasoner {

    /** Reason: the task has no candidate resource. */
    public static final String NO_ELIGIBLE_RESOURCE = "NO_ELIGIBLE_RESOURCE";
    /** Reason: the start domain is empty. */
    public static final String OUTSIDE_WINDOW = "OUTSIDE_WINDOW";
    /** Reason: an IMPLIES predecessor is not selected. */
    public static final String PREDECESSOR_NOT_SELECTED = "PREDECESSOR_NOT_SELECTED";
    /** Reason: the optimiser preferred other work. */
    public static final String NOT_SELECTED = "NOT_SELECTED";

    /** Hard constraint families that prune candidates/starts in the neutral compile step (design 6.10 A). */
    private static final Set<String> PRUNING_TYPES =
            Set.of("task_eligibility", "fixed_assignment", "time_window", "resource_availability");

    private final PlanningProblem problem;
    private final CandidateIndexView index;
    private final Task[] tasks;
    private final int[][] impliesPredecessors;

    /**
     * @param problem the normalized problem
     * @param index   full index view
     */
    public UnselectedReasoner(PlanningProblem problem, CandidateIndexView index) {
        this.problem = problem;
        this.index = index;
        this.tasks = new Task[index.taskCount()];
        for (Task task : problem.tasks()) {
            int t = index.taskIdx(task.id());
            if (t >= 0) {
                tasks[t] = task;
            }
        }
        this.impliesPredecessors = new int[tasks.length][];
        for (int t = 0; t < tasks.length; t++) {
            Task task = tasks[t];
            int[] preds = new int[task == null ? 0 : task.predecessors().size()];
            int n = 0;
            if (task != null) {
                for (PrecedenceRef ref : task.predecessors()) {
                    int p = index.taskIdx(ref.taskId());
                    if (p >= 0 && ref.presence() == PrecedenceRef.Presence.IMPLIES) {
                        preds[n++] = p;
                    }
                }
            }
            impliesPredecessors[t] = Arrays.copyOf(preds, n);
        }
    }

    /**
     * @param solution the solution view
     * @return one entry per unselected OPTIONAL task, sorted by task id
     */
    public List<UnassignedTask> reason(SolutionView solution) {
        List<UnassignedTask> out = new ArrayList<>();
        for (int t = 0; t < tasks.length; t++) {
            Task task = tasks[t];
            if (task == null || task.requiredTask() || solution.selected(t)) {
                continue;
            }
            out.add(reasonFor(solution, t, task));
        }
        out.sort(Comparator.comparing(UnassignedTask::taskId));
        return out;
    }

    private UnassignedTask reasonFor(SolutionView solution, int t, Task task) {
        long prize = task.effectivePrize();
        if (index.candidateEnd(t) - index.candidateBegin(t) == 0) {
            return new UnassignedTask(task.id(), NO_ELIGIBLE_RESOURCE,
                    "no resource can fulfil the requirements of task '" + task.id() + "'",
                    "Check the resource selectors, skills, calendars and hard constraints" + pruners(t),
                    prize);
        }
        if (index.estStart(t) > index.lstStart(t)) {
            return new UnassignedTask(task.id(), OUTSIDE_WINDOW,
                    "no feasible start time exists for task '" + task.id() + "' inside its time window",
                    "Widen the release/deadline window or the resource calendars", prize);
        }
        for (int p : impliesPredecessors[t]) {
            if (!solution.selected(p)) {
                return new UnassignedTask(task.id(), PREDECESSOR_NOT_SELECTED,
                        "predecessor '" + index.taskId(p) + "' of task '" + task.id() + "' is not selected",
                        "Make the predecessor selectable or remove the IMPLIES link", prize);
            }
        }
        return new UnassignedTask(task.id(), NOT_SELECTED,
                "not selected by the optimiser: the capacity or time it needs was worth more elsewhere",
                "Increase its prize/priority or add capacity", prize);
    }

    /** @return " (possibly pruned by: id1, id2)" for the hard pruning constraints scoping the task, else "" */
    private String pruners(int t) {
        List<String> ids = new ArrayList<>();
        for (Constraint constraint : problem.constraints()) {
            if (constraint.enforcement() == IEnumEnforcement.Types.SOFT
                    || constraint.type() == null
                    || !PRUNING_TYPES.contains(constraint.type().trim().toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            int[] scoped = index.resolveTasks(constraint.scope());
            if (scoped != null && Arrays.binarySearch(scoped, t) >= 0) {
                ids.add(constraint.id());
            }
        }
        return ids.isEmpty() ? "" : " (possibly pruned by: " + String.join(", ", ids) + ")";
    }
}

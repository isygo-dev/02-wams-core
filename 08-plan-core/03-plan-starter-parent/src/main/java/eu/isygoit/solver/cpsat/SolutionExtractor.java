package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.solver.index.CandidateIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads a CP-SAT solution into the result IR (design 6.14): selected tasks become {@link Assignment}s in ticks AND
 * instants, unselected optional tasks become {@link UnassignedTask}s.
 * <p>
 * The values are read through {@link Values}, implemented over a finished {@code CpSolver} (final solution) or over the
 * solution callback (incumbent snapshots), so one extraction routine serves both. Complexity O(T + C).
 * <p>
 * Thread-safety: stateless utility. Ordering: assignments sorted by {@code (startTick, taskId)}, unassigned tasks by task
 * id (determinism, design 8.3). Units: ticks and horizon instants.
 */
public final class SolutionExtractor {

    /** Reason code of an optional task the solver chose not to select (the validator may refine it, design 8.4). */
    public static final String REASON_NOT_SELECTED = "NOT_SELECTED";

    private SolutionExtractor() {
    }

    /** Read access to the values of one solution. */
    public interface Values {
        /** @return the value of an integer variable or linear expression */
        long value(LinearArgument expression);

        /** @return the value of a literal */
        boolean isTrue(Literal literal);

        /**
         * @param solver a solver that holds a solution
         * @return values read from the solver
         */
        static Values of(CpSolver solver) {
            return new Values() {
                @Override
                public long value(LinearArgument expression) {
                    return solver.value(expression);
                }

                @Override
                public boolean isTrue(Literal literal) {
                    return solver.booleanValue(literal);
                }
            };
        }

        /**
         * @param callback a solution callback inside {@code onSolutionCallback}
         * @return values read from the callback
         */
        static Values of(CpSolverSolutionCallback callback) {
            return new Values() {
                @Override
                public long value(LinearArgument expression) {
                    return callback.value(expression);
                }

                @Override
                public boolean isTrue(Literal literal) {
                    return callback.booleanValue(literal);
                }
            };
        }
    }

    /**
     * The extracted plan.
     *
     * @param assignments the selected tasks sorted by start tick then task id
     * @param unassigned  the unselected optional tasks sorted by task id
     */
    public record Extracted(List<Assignment> assignments, List<UnassignedTask> unassigned) {
    }

    /**
     * Extracts assignments and unselected tasks.
     *
     * @param ctx    the model context of the solved model
     * @param values the solution values
     * @return the extracted plan
     */
    public static Extracted extract(CpModelContext ctx, Values values) {
        CandidateIndex index = ctx.index();
        List<Assignment> assignments = new ArrayList<>();
        List<UnassignedTask> unassigned = new ArrayList<>();
        for (int t = 0; t < index.taskCount(); t++) {
            if (values.isTrue(ctx.presence(t))) {
                assignments.add(assignmentOf(ctx, t, values));
            } else {
                unassigned.add(new UnassignedTask(index.taskId(t), REASON_NOT_SELECTED,
                        "Optional task was not selected by the solver", null, index.prize(t)));
            }
        }
        assignments.sort(Comparator.comparingInt(Assignment::startTick).thenComparing(Assignment::taskId));
        unassigned.sort(Comparator.comparing(UnassignedTask::taskId));
        return new Extracted(assignments, unassigned);
    }

    /**
     * Extracts only the assignments (cheaper; used for incumbent snapshots).
     *
     * @param ctx    the model context
     * @param values the solution values
     * @return the assignments sorted by start tick then task id
     */
    public static List<Assignment> assignments(CpModelContext ctx, Values values) {
        CandidateIndex index = ctx.index();
        List<Assignment> assignments = new ArrayList<>();
        for (int t = 0; t < index.taskCount(); t++) {
            if (values.isTrue(ctx.presence(t))) {
                assignments.add(assignmentOf(ctx, t, values));
            }
        }
        assignments.sort(Comparator.comparingInt(Assignment::startTick).thenComparing(Assignment::taskId));
        return assignments;
    }

    private static Assignment assignmentOf(CpModelContext ctx, int t, Values values) {
        CandidateIndex index = ctx.index();
        int start = Math.toIntExact(values.value(ctx.start(t)));
        List<ResourceAssignment> resources = new ArrayList<>();
        int duration = -1;
        int begin = index.candidateBegin(t);
        int end = index.candidateEnd(t);
        for (int q = index.taskReqStart[t]; q < index.taskReqStart[t + 1]; q++) {
            for (int c = begin; c < end; c++) {
                if (index.candReq[c] == q && values.isTrue(ctx.cand(c))) {
                    resources.add(new ResourceAssignment(index.resourceId(index.candidateResource(c)),
                            index.roles[index.reqRole[q]], index.candidateAmount(c)));
                    if (duration < 0) {
                        duration = index.candidateDuration(c);
                    }
                }
            }
        }
        if (duration < 0) {
            duration = index.duration(t);
        }
        int endTick = start + duration;
        return new Assignment(index.taskId(t), !index.required(t), start, endTick,
                index.horizon().toInstant(start), index.horizon().toInstant(endTick), slotOf(index, t, start, duration),
                resources);
    }

    /** The slot whose start equals the task start (slot mode), preferring one that contains the whole task. */
    private static String slotOf(CandidateIndex index, int t, int start, int duration) {
        int from = index.taskSlotStart[t];
        int to = index.taskSlotStart[t + 1];
        String fallback = null;
        for (int i = from; i < to; i++) {
            int slot = index.taskSlots[i];
            if (index.slotStart[slot] == start) {
                if (index.slotEnd[slot] - start >= duration) {
                    return index.slotId(slot);
                }
                if (fallback == null) {
                    fallback = index.slotId(slot);
                }
            }
        }
        return fallback;
    }
}

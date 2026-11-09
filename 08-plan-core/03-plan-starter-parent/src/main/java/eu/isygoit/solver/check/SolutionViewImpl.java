package eu.isygoit.solver.check;

import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.SolutionView;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable int-indexed {@link SolutionView} built ONCE per validation from the assignments of a solver result
 * (design 5.2, 8.2 step 1). No solver object is touched.
 * <p>
 * Indices are those of the {@link CandidateIndexView} (request order). Building is O(A log A + R): the per-resource task
 * lists are produced by a counting pass followed by a sort of each (small) list by (start, task index). A result that
 * cannot even be indexed (unknown task/resource/slot id, a task assigned twice) is rejected with
 * {@link MalformedSolutionException} carrying a {@code SOLUTION_INVALID_*} code; the validator converts it into an
 * {@code InvalidSolverResultException}.
 * <p>
 * Thread-safe once built (arrays are never modified; callers must not modify the arrays they receive).
 */
public final class SolutionViewImpl implements SolutionView {

    /** Raised when the assignments cannot be mapped onto the index. */
    public static final class MalformedSolutionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String code;

        MalformedSolutionException(String code, String message) {
            super(message);
            this.code = code;
        }

        /** @return a {@code SOLUTION_INVALID_*} code */
        public String code() {
            return code;
        }
    }

    private static final int[] EMPTY = new int[0];

    private final int taskCount;
    private final boolean[] selected;
    private final int[] start;
    private final int[] end;
    private final int[][] resources;
    private final String[][] roles;
    private final int[][] amounts;
    private final int[] slot;
    private final int[][] byResource;

    private SolutionViewImpl(int taskCount, boolean[] selected, int[] start, int[] end, int[][] resources,
                             String[][] roles, int[][] amounts, int[] slot, int[][] byResource) {
        this.taskCount = taskCount;
        this.selected = selected;
        this.start = start;
        this.end = end;
        this.resources = resources;
        this.roles = roles;
        this.amounts = amounts;
        this.slot = slot;
        this.byResource = byResource;
    }

    /**
     * @param index       read-only index view (always-available methods only are used)
     * @param assignments the assignments of the result (may be empty, never {@code null})
     * @return the view
     * @throws MalformedSolutionException unknown ids or a task assigned more than once
     */
    public static SolutionViewImpl build(CandidateIndexView index, List<Assignment> assignments) {
        int taskCount = index.taskCount();
        int resourceCount = index.resourceCount();
        boolean[] selected = new boolean[taskCount];
        int[] start = new int[taskCount];
        int[] end = new int[taskCount];
        int[][] res = new int[taskCount][];
        String[][] roles = new String[taskCount][];
        int[][] amounts = new int[taskCount][];
        int[] slot = new int[taskCount];
        Arrays.fill(slot, -1);
        Arrays.fill(res, EMPTY);
        Arrays.fill(amounts, EMPTY);
        Arrays.fill(roles, new String[0]);

        int[] perResource = new int[resourceCount];
        for (Assignment assignment : assignments) {
            int t = index.taskIdx(assignment.taskId());
            if (t < 0) {
                throw new MalformedSolutionException("SOLUTION_INVALID_TASK_UNKNOWN",
                        "Solver returned an assignment for unknown task '" + assignment.taskId() + "'");
            }
            if (selected[t]) {
                throw new MalformedSolutionException("SOLUTION_INVALID_TASK_DUPLICATE",
                        "Solver assigned task '" + assignment.taskId() + "' more than once");
            }
            selected[t] = true;
            start[t] = assignment.startTick();
            end[t] = assignment.endTick();
            if (assignment.slotId() != null) {
                int s = index.slotIdx(assignment.slotId());
                if (s < 0) {
                    throw new MalformedSolutionException("SOLUTION_INVALID_SLOT_UNKNOWN",
                            "Solver returned unknown slot '" + assignment.slotId() + "' for task '"
                                    + assignment.taskId() + "'");
                }
                slot[t] = s;
            }
            List<ResourceAssignment> list = assignment.resources();
            int[] r = new int[list.size()];
            String[] ro = new String[list.size()];
            int[] am = new int[list.size()];
            for (int k = 0; k < r.length; k++) {
                ResourceAssignment ra = list.get(k);
                int ridx = index.resourceIdx(ra.resourceId());
                if (ridx < 0) {
                    throw new MalformedSolutionException("SOLUTION_INVALID_RESOURCE_UNKNOWN",
                            "Solver returned an unknown resource '" + ra.resourceId() + "' for task '"
                                    + assignment.taskId() + "'");
                }
                r[k] = ridx;
                ro[k] = ra.role();
                am[k] = ra.amount();
                perResource[ridx]++;
            }
            res[t] = r;
            roles[t] = ro;
            amounts[t] = am;
        }

        // per-resource task lists: counting pass then fill in task-index order, then sort by (start, task idx)
        int[][] byResource = new int[resourceCount][];
        for (int r = 0; r < resourceCount; r++) {
            byResource[r] = perResource[r] == 0 ? EMPTY : new int[perResource[r]];
        }
        int[] fill = new int[resourceCount];
        for (int t = 0; t < taskCount; t++) {
            if (!selected[t]) {
                continue;
            }
            int[] used = res[t];
            for (int k = 0; k < used.length; k++) {
                int r = used[k];
                // a resource listed twice for one task is reported by the structural checker; list it once here
                if (fill[r] > 0 && byResource[r][fill[r] - 1] == t) {
                    continue;
                }
                byResource[r][fill[r]++] = t;
            }
        }
        for (int r = 0; r < resourceCount; r++) {
            int[] list = fill[r] == byResource[r].length ? byResource[r] : Arrays.copyOf(byResource[r], fill[r]);
            sortByStart(list, start);
            byResource[r] = list;
        }
        return new SolutionViewImpl(taskCount, selected, start, end, res, roles, amounts, slot, byResource);
    }

    /** Sorts task indices by (start, index): encode as long, sort primitives. */
    private static void sortByStart(int[] tasks, int[] start) {
        if (tasks.length < 2) {
            return;
        }
        long[] keys = new long[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            keys[i] = (((long) start[tasks[i]]) << 32) | (tasks[i] & 0xFFFFFFFFL);
        }
        Arrays.sort(keys);
        for (int i = 0; i < tasks.length; i++) {
            tasks[i] = (int) keys[i];
        }
    }

    @Override
    public int taskCount() {
        return taskCount;
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
        return resources[task].length;
    }

    @Override
    public int resourceAt(int task, int k) {
        return resources[task][k];
    }

    @Override
    public String roleAt(int task, int k) {
        return roles[task][k];
    }

    @Override
    public int amountAt(int task, int k) {
        return amounts[task][k];
    }

    @Override
    public int[] tasksOfResource(int resource) {
        return byResource[resource];
    }

    @Override
    public int slotOf(int task) {
        return slot[task];
    }
}

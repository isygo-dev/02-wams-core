package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.LoadObjectiveParams;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.util.ScaledMath;

import java.util.HashMap;
import java.util.Map;

/**
 * Arithmetic and data helpers shared by the objective evaluators (this package) and the CP-SAT contributors
 * ({@code solver.cpsat.objective}), so that the model value and the independent checker value are computed from the SAME
 * definitions (design 8.2: "model value == checker value").
 * <p>
 * Stateless static helpers (plus the immutable {@link SetupLookup}); thread-safe. Units: ticks for times, scaled integers
 * for decimal inputs, exact integers for counts.
 */
public final class ObjectiveMath {

    /** Marker of "no due date" in {@link CandidateIndexView#due(int)}. */
    public static final int NO_DUE = Integer.MIN_VALUE;

    private ObjectiveMath() {
    }

    /**
     * @return the effective decimal-to-long multiplier of an objective: {@code objective.scale()} or, when the normalizer
     * has not materialised it, {@code options.objectiveScale}
     */
    public static int scaleOf(PlanningProblem problem, Objective objective) {
        if (objective.scale() != null) {
            return objective.scale();
        }
        return problem.options().objectiveScale();
    }

    /** @return the load metric of the (possibly {@code null}) bound params, default TICKS */
    public static LoadMetric metricOf(Object params) {
        if (params instanceof LoadObjectiveParams p && p.metric() != null) {
            return p.metric();
        }
        return LoadMetric.TICKS;
    }

    /** @return {@code max(0, end - due)} */
    public static long tardiness(int end, int due) {
        return Math.max(0L, (long) end - (long) due);
    }

    /**
     * @return the smallest duration the task can have over its candidates (the base duration when it has none)
     */
    public static int minDuration(CandidateIndexView index, int task) {
        int begin = index.candidateBegin(task);
        int end = index.candidateEnd(task);
        if (begin >= end) {
            return index.duration(task);
        }
        int min = Integer.MAX_VALUE;
        for (int c = begin; c < end; c++) {
            min = Math.min(min, index.candidateDuration(c));
        }
        return min;
    }

    /** @return a mask of length {@code resourceCount} with {@code true} at the given resource indices */
    public static boolean[] mask(int resourceCount, int[] resources) {
        boolean[] mask = new boolean[resourceCount];
        for (int r : resources) {
            mask[r] = true;
        }
        return mask;
    }

    /** @return {@code mask[r] == true} iff at least one surviving candidate uses resource {@code r} (O(C)) */
    public static boolean[] resourcesWithCandidates(CandidateIndexView index) {
        boolean[] has = new boolean[index.resourceCount()];
        for (int t = 0; t < index.taskCount(); t++) {
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                has[index.candidateResource(c)] = true;
            }
        }
        return has;
    }

    /** @return the candidate ids of every resource, ascending (two passes, O(C)) */
    public static int[][] candidatesByResource(CandidateIndexView index) {
        int resources = index.resourceCount();
        int[] counts = new int[resources];
        for (int t = 0; t < index.taskCount(); t++) {
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                counts[index.candidateResource(c)]++;
            }
        }
        int[][] result = new int[resources][];
        for (int r = 0; r < resources; r++) {
            result[r] = new int[counts[r]];
        }
        int[] fill = new int[resources];
        for (int t = 0; t < index.taskCount(); t++) {
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                int r = index.candidateResource(c);
                result[r][fill[r]++] = c;
            }
        }
        return result;
    }

    /**
     * Valid upper bound of the load of every resource: the sum of the candidate durations (TICKS) or the number of
     * candidates (COUNT), capped by {@code maxLoadTicks} for TICKS.
     */
    public static long[] loadUpperBounds(CandidateIndexView index, LoadMetric metric) {
        long[] bound = new long[index.resourceCount()];
        for (int t = 0; t < index.taskCount(); t++) {
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                int r = index.candidateResource(c);
                bound[r] += metric == LoadMetric.TICKS ? index.candidateDuration(c) : 1L;
            }
        }
        if (metric == LoadMetric.TICKS) {
            for (int r = 0; r < bound.length; r++) {
                int cap = index.maxLoadTicks(r);
                if (cap != Integer.MAX_VALUE) {
                    bound[r] = Math.min(bound[r], Math.max(0, cap));
                }
            }
        }
        return bound;
    }

    /** @return the load of a resource in a finished solution: summed (end - start) of its tasks, or their number */
    public static long load(SolutionView solution, int resource, LoadMetric metric) {
        long load = 0;
        for (int t : solution.tasksOfResource(resource)) {
            load += metric == LoadMetric.TICKS ? (long) solution.end(t) - solution.start(t) : 1L;
        }
        return load;
    }

    /**
     * Scaled price of a slot: {@code ScaledMath.scale(slot.cost, scale)}.
     *
     * @throws eu.isygoit.exception.ObjectiveRangeException when the cost is negative, not finite or too large
     */
    public static long slotCost(PlanningProblem problem, int slot, int scale) {
        return ScaledMath.scale(problem.slots().get(slot).cost(), scale);
    }

    /**
     * Setup duration lookup between two tasks on a resource, computed straight from the problem (matrices, task setup
     * classes) so the checker does not depend on the solver index. Rules: no setup class on either task or no matrix on the
     * resource gives 0; an explicit {@code (from, to)} entry wins; the same class gives {@code sameClassTicks}; otherwise
     * {@code defaultTicks}. Immutable and thread-safe.
     */
    public static final class SetupLookup {
        private final PlanningProblem problem;
        private final Map<String, SetupMatrix> matrices = new HashMap<>();
        private final Map<String, Map<String, Integer>> entries = new HashMap<>();

        private SetupLookup(PlanningProblem problem) {
            this.problem = problem;
            for (SetupMatrix m : problem.setupMatrices()) {
                matrices.putIfAbsent(m.id(), m);
                Map<String, Integer> byPair = new HashMap<>();
                for (SetupEntry e : m.entries()) {
                    byPair.putIfAbsent(e.from() + '\u0000' + e.to(), e.ticks());
                }
                entries.putIfAbsent(m.id(), byPair);
            }
        }

        /** @return the lookup of a problem */
        public static SetupLookup of(PlanningProblem problem) {
            return new SetupLookup(problem);
        }

        /** @return {@code true} when the resource has a setup matrix */
        public boolean hasMatrix(int resource) {
            String id = problem.resources().get(resource).setupMatrixId();
            return id != null && matrices.containsKey(id);
        }

        /**
         * @param resource resource index
         * @param fromTask task index of the earlier task
         * @param toTask   task index of the immediate successor
         * @return the setup ticks between the end of {@code fromTask} and the start of {@code toTask}
         */
        public int ticks(int resource, int fromTask, int toTask) {
            String matrixId = problem.resources().get(resource).setupMatrixId();
            if (matrixId == null) {
                return 0;
            }
            SetupMatrix matrix = matrices.get(matrixId);
            if (matrix == null) {
                return 0;
            }
            String from = problem.tasks().get(fromTask).setupClass();
            String to = problem.tasks().get(toTask).setupClass();
            if (from == null || to == null) {
                return 0;
            }
            Integer explicit = entries.get(matrixId).get(from + '\u0000' + to);
            if (explicit != null) {
                return explicit;
            }
            return from.equals(to) ? matrix.sameClassTicks() : matrix.defaultTicks();
        }
    }
}

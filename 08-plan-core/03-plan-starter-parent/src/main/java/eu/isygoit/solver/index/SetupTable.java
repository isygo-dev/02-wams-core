package eu.isygoit.solver.index;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Setup (sequence-dependent changeover) times between task classes per resource (design 2.2, 6.6).
 * <p>
 * {@code ticks(r, from, to)} is the idle time resource {@code r} needs between the END of a task of class {@code from}
 * and the START of a task of class {@code to} when {@code to} immediately follows. Resolution order: explicit entry
 * {@code (from, to)} of the resource's matrix, else {@code sameClassTicks} when both classes are equal, else
 * {@code defaultTicks}. A resource without matrix, or a task without class ({@code -1}), has no setup (0). Classes are
 * the indices of {@link CandidateIndex#setupClassNames}. Sparse storage (no K x K table); immutable and thread-safe.
 */
public final class SetupTable {

    private final int[] resourceMatrix;
    private final int[] defaultTicks;
    private final int[] sameClassTicks;
    private final List<Map<Long, Integer>> overrides;
    private final int maxTicks;

    private SetupTable(int[] resourceMatrix, int[] defaultTicks, int[] sameClassTicks,
                       List<Map<Long, Integer>> overrides, int maxTicks) {
        this.resourceMatrix = resourceMatrix;
        this.defaultTicks = defaultTicks;
        this.sameClassTicks = sameClassTicks;
        this.overrides = overrides;
        this.maxTicks = maxTicks;
    }

    /**
     * Builds the table.
     *
     * @param index   the candidate index (resource matrices and class dictionary)
     * @param problem the problem whose {@code setupMatrices} the index refers to
     * @return the table
     */
    public static SetupTable build(CandidateIndex index, PlanningProblem problem) {
        Map<String, Integer> classIndex = new HashMap<>();
        for (int c = 0; c < index.setupClassNames.length; c++) {
            classIndex.put(index.setupClassNames[c], c);
        }
        List<SetupMatrix> matrices = problem.setupMatrices();
        int n = matrices.size();
        int[] def = new int[n];
        int[] same = new int[n];
        List<Map<Long, Integer>> sparse = new ArrayList<>(n);
        int max = 0;
        for (int m = 0; m < n; m++) {
            SetupMatrix matrix = matrices.get(m);
            def[m] = matrix.defaultTicks();
            same[m] = matrix.sameClassTicks();
            max = Math.max(max, Math.max(def[m], same[m]));
            Map<Long, Integer> entries = new HashMap<>();
            for (SetupEntry entry : matrix.entries()) {
                Integer from = classIndex.get(entry.from());
                Integer to = classIndex.get(entry.to());
                if (from != null && to != null) {
                    entries.put(key(from, to), entry.ticks());
                    max = Math.max(max, entry.ticks());
                }
            }
            sparse.add(entries);
        }
        return new SetupTable(index.resSetupMatrix, def, same, sparse, max);
    }

    /**
     * Setup time between two consecutive classes on a resource.
     *
     * @param resource  resource index
     * @param classFrom class of the predecessor task, or {@code -1}
     * @param classTo   class of the successor task, or {@code -1}
     * @return the setup ticks, {@code 0} when the resource has no matrix or a class is missing
     */
    public int ticks(int resource, int classFrom, int classTo) {
        int m = resourceMatrix[resource];
        if (m < 0 || classFrom < 0 || classTo < 0) {
            return 0;
        }
        Integer explicit = overrides.get(m).get(key(classFrom, classTo));
        if (explicit != null) {
            return explicit;
        }
        return classFrom == classTo ? sameClassTicks[m] : defaultTicks[m];
    }

    /** @return {@code true} when the resource has a setup matrix */
    public boolean hasSetup(int resource) {
        return resourceMatrix[resource] >= 0;
    }

    /** @return the largest setup time of any matrix (upper bound of {@link #ticks}) */
    public int maxTicks() {
        return maxTicks;
    }

    private static long key(int from, int to) {
        return (((long) from) << 32) | (to & 0xFFFFFFFFL);
    }
}

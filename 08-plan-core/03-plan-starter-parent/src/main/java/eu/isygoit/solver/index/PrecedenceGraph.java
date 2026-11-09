package eu.isygoit.solver.index;

import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph algorithms over the hard precedence edges ({@code Task.predecessors}): deterministic topological order, cycle
 * path extraction for error messages and critical-path (CPM) tightening of start windows (design 5.4 steps 5-6, 7.1).
 * <p>
 * Edge type codes are the ordinals of {@link PrecedenceRef.Type}: 0 = FINISH_START, 1 = START_START,
 * 2 = FINISH_FINISH, 3 = START_FINISH. All functions are pure and O(T + E).
 */
public final class PrecedenceGraph {

    /** Edge type code of {@link PrecedenceRef.Type#FINISH_START}. */
    public static final int FINISH_START = PrecedenceRef.Type.FINISH_START.ordinal();
    /** Edge type code of {@link PrecedenceRef.Type#START_START}. */
    public static final int START_START = PrecedenceRef.Type.START_START.ordinal();
    /** Edge type code of {@link PrecedenceRef.Type#FINISH_FINISH}. */
    public static final int FINISH_FINISH = PrecedenceRef.Type.FINISH_FINISH.ordinal();
    /** Edge type code of {@link PrecedenceRef.Type#START_FINISH}. */
    public static final int START_FINISH = PrecedenceRef.Type.START_FINISH.ordinal();

    private static final long BOUND = Integer.MAX_VALUE / 2;

    private PrecedenceGraph() {
    }

    /**
     * Kahn topological order, deterministic (ready tasks are taken first-in-first-out, seeded in ascending index).
     *
     * @param taskCount number of tasks
     * @param edgePred  predecessor task index of each edge
     * @param edgeSucc  successor task index of each edge (same length as {@code edgePred})
     * @return a permutation of {@code 0..taskCount-1} where every predecessor comes before its successors, or
     * {@code null} when the graph contains a cycle
     */
    public static int[] topologicalOrder(int taskCount, int[] edgePred, int[] edgeSucc) {
        int edges = edgePred.length;
        int[] indegree = new int[taskCount];
        int[] outStart = new int[taskCount + 1];
        for (int e = 0; e < edges; e++) {
            indegree[edgeSucc[e]]++;
            outStart[edgePred[e] + 1]++;
        }
        for (int t = 0; t < taskCount; t++) {
            outStart[t + 1] += outStart[t];
        }
        int[] fill = new int[taskCount];
        int[] outEdges = new int[edges];
        for (int e = 0; e < edges; e++) {
            int p = edgePred[e];
            outEdges[outStart[p] + fill[p]++] = edgeSucc[e];
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
            int t = order[head++];
            for (int k = outStart[t]; k < outStart[t + 1]; k++) {
                int s = outEdges[k];
                if (--indegree[s] == 0) {
                    order[tail++] = s;
                }
            }
        }
        return tail == taskCount ? order : null;
    }

    /**
     * Finds one precedence cycle among the tasks (references to unknown task ids and self references are ignored: they
     * are reported by their own validation codes).
     *
     * @param tasks the tasks in request order
     * @return the task ids of one cycle in precedence order ({@code [A, B, C]} means {@code A -> B -> C -> A}), or an
     * empty list when the graph is acyclic
     */
    public static List<String> findCycle(List<Task> tasks) {
        int n = tasks.size();
        Map<String, Integer> index = new HashMap<>();
        for (int t = 0; t < n; t++) {
            if (tasks.get(t).id() != null) {
                index.putIfAbsent(tasks.get(t).id(), t);
            }
        }
        List<int[]> edges = new ArrayList<>();
        for (int t = 0; t < n; t++) {
            for (PrecedenceRef ref : tasks.get(t).predecessors()) {
                Integer p = ref.taskId() == null ? null : index.get(ref.taskId());
                if (p != null && p != t) {
                    edges.add(new int[]{p, t});
                }
            }
        }
        int[] pred = new int[edges.size()];
        int[] succ = new int[edges.size()];
        for (int e = 0; e < edges.size(); e++) {
            pred[e] = edges.get(e)[0];
            succ[e] = edges.get(e)[1];
        }
        // Kahn to find the residual (cyclic) part
        int[] indegree = new int[n];
        int[] outStart = new int[n + 1];
        for (int e = 0; e < pred.length; e++) {
            indegree[succ[e]]++;
            outStart[pred[e] + 1]++;
        }
        for (int t = 0; t < n; t++) {
            outStart[t + 1] += outStart[t];
        }
        int[] fill = new int[n];
        int[] outEdges = new int[pred.length];
        for (int e = 0; e < pred.length; e++) {
            outEdges[outStart[pred[e]] + fill[pred[e]]++] = succ[e];
        }
        boolean[] removed = new boolean[n];
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        for (int t = 0; t < n; t++) {
            if (indegree[t] == 0) {
                queue[tail++] = t;
            }
        }
        while (head < tail) {
            int t = queue[head++];
            removed[t] = true;
            for (int k = outStart[t]; k < outStart[t + 1]; k++) {
                if (--indegree[outEdges[k]] == 0) {
                    queue[tail++] = outEdges[k];
                }
            }
        }
        if (tail == n) {
            return List.of();
        }
        // every residual task has a residual predecessor: walk predecessors until a task repeats
        List<List<Integer>> predsOf = new ArrayList<>();
        for (int t = 0; t < n; t++) {
            predsOf.add(new ArrayList<>());
        }
        for (int e = 0; e < pred.length; e++) {
            predsOf.get(succ[e]).add(pred[e]);
        }
        int start = -1;
        for (int t = 0; t < n; t++) {
            if (!removed[t]) {
                start = t;
                break;
            }
        }
        int[] position = new int[n];
        java.util.Arrays.fill(position, -1);
        List<Integer> walk = new ArrayList<>();
        int current = start;
        while (position[current] < 0) {
            position[current] = walk.size();
            walk.add(current);
            int next = -1;
            for (int p : predsOf.get(current)) {
                if (!removed[p]) {
                    next = p;
                    break;
                }
            }
            current = next;
        }
        List<Integer> cycle = new ArrayList<>(walk.subList(position[current], walk.size()));
        Collections.reverse(cycle); // walk follows predecessors; precedence order is the reverse
        List<String> ids = new ArrayList<>(cycle.size());
        for (int t : cycle) {
            ids.add(tasks.get(t).id());
        }
        return ids;
    }

    /**
     * Critical-path tightening of start windows over the hard edges (min lags only; max lags are enforced by the
     * model). A forward bound is only applied when the predecessor is required (always present), a backward bound
     * only when the successor is required, so optional tasks are never over-restricted.
     * <p>
     * Edge semantics ({@code d} = duration, {@code lag} = minLag): FS {@code s_j >= s_i + d_i + lag}; SS
     * {@code s_j >= s_i + lag}; FF {@code s_j + d_j >= s_i + d_i + lag}; SF {@code s_j + d_j >= s_i + lag}. Bounds use
     * {@code minDur} for the predecessor side and {@code maxDur} for the successor side so they stay valid.
     *
     * @param est           in/out earliest start per task
     * @param lst           in/out latest start per task
     * @param minDur        smallest duration per task
     * @param maxDur        largest duration per task
     * @param required      whether the task is always present
     * @param succEdgeStart CSR offsets of the incoming edges by successor ({@code T + 1} entries)
     * @param edgePred      predecessor of each edge
     * @param edgeType      edge type code (ordinal of {@link PrecedenceRef.Type})
     * @param edgeMinLag    minimum lag in ticks (may be negative)
     * @param topoOrder     topological order of the tasks
     * @return the number of tasks whose window is empty ({@code est > lst}) after propagation
     */
    public static int propagateWindows(int[] est, int[] lst, int[] minDur, int[] maxDur, boolean[] required,
                                       int[] succEdgeStart, int[] edgePred, int[] edgeType, int[] edgeMinLag,
                                       int[] topoOrder) {
        for (int k = 0; k < topoOrder.length; k++) {
            int j = topoOrder[k];
            for (int e = succEdgeStart[j]; e < succEdgeStart[j + 1]; e++) {
                int i = edgePred[e];
                if (!required[i]) {
                    continue;
                }
                long bound;
                switch (edgeType[e]) {
                    case 0:
                        bound = (long) est[i] + minDur[i] + edgeMinLag[e];
                        break;
                    case 1:
                        bound = (long) est[i] + edgeMinLag[e];
                        break;
                    case 2:
                        bound = (long) est[i] + minDur[i] + edgeMinLag[e] - maxDur[j];
                        break;
                    default:
                        bound = (long) est[i] + edgeMinLag[e] - maxDur[j];
                        break;
                }
                est[j] = Math.max(est[j], clamp(bound));
            }
        }
        for (int k = topoOrder.length - 1; k >= 0; k--) {
            int j = topoOrder[k];
            if (!required[j]) {
                continue;
            }
            for (int e = succEdgeStart[j]; e < succEdgeStart[j + 1]; e++) {
                int i = edgePred[e];
                long bound;
                switch (edgeType[e]) {
                    case 0:
                        bound = (long) lst[j] - minDur[i] - edgeMinLag[e];
                        break;
                    case 1:
                        bound = (long) lst[j] - edgeMinLag[e];
                        break;
                    case 2:
                        bound = (long) lst[j] + maxDur[j] - minDur[i] - edgeMinLag[e];
                        break;
                    default:
                        bound = (long) lst[j] + maxDur[j] - edgeMinLag[e];
                        break;
                }
                lst[i] = Math.min(lst[i], clamp(bound));
            }
        }
        int empty = 0;
        for (int t = 0; t < est.length; t++) {
            if (est[t] > lst[t]) {
                empty++;
            }
        }
        return empty;
    }

    private static int clamp(long v) {
        return (int) Math.max(-BOUND, Math.min(BOUND, v));
    }
}

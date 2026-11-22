package eu.isygoit.solver.diag;

import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.spi.CandidateIndexView;

import java.util.*;

/**
 * Capacity proof over GROUPS of interchangeable resources (Hall's condition on the compiled index).
 * <p>
 * {@link PreSolveAnalyzer} already proves that ONE resource cannot carry the work only it can do. This class proves the
 * same for a SET of resources: the required tasks that can only be served by resources of the set need more work than
 * the set can offer, whatever the assignment is.
 * <p>
 * The relaxation is sound (it never rejects a problem that has a solution):
 * <ul>
 *   <li>every required (task, requirement) pair needs {@code count x min(duration x amount)} work units from the
 *       resources of its surviving candidates;</li>
 *   <li>a resource offers {@code capacity x |U|} units, where {@code U} is the union of the ticks its candidates can
 *       occupy given their start domains (calendars, slots, windows), bounded by its available ticks and by its
 *       {@code maxLoadTicks};</li>
 *   <li>a maximum flow from the work to the resources tells whether the work fits; when it does not, the part of the
 *       network still reachable from the source in the residual graph is a violated set, split in independent
 *       components.</li>
 * </ul>
 * Nothing is reported when the structure cannot list the candidates of a requirement or when the problem is too large
 * for a quick pre-solve check. Stateless and thread-safe.
 */
final class GroupCapacity {

    /**
     * Upper bound on the number of candidate occupancy intervals examined; beyond it the check is skipped.
     */
    static final int MAX_INTERVALS = 4_000_000;

    private static final long INFINITE = Long.MAX_VALUE / 4;

    private GroupCapacity() {
    }

    /**
     * A violated set of resources.
     *
     * @param resources the resources of the set (ascending index)
     * @param tasks     the required tasks that can only be served by resources of the set (ascending index)
     * @param roles     the requirement roles involved
     * @param demand    work units (ticks x amount) the tasks need
     * @param supply    work units the resources can offer together
     */
    record Violation(int[] resources, int[] tasks, Set<String> roles, long demand, long supply) {
        long deficit() {
            return demand - supply;
        }
    }

    /**
     * Searches the violated resource sets.
     *
     * @param index     the index view
     * @param structure requirement-level data
     * @param tasks     the tasks by index (entries may be {@code null})
     * @param skipped   task indexes already reported, not counted again
     * @return the violations, largest deficit first; empty when none was proven or the check does not apply
     */
    static List<Violation> find(CandidateIndexView index, PreSolveAnalyzer.Structure structure, Task[] tasks,
                                Set<Integer> skipped) {
        Network network = Network.build(index, structure, tasks, skipped);
        return network == null ? List.of() : network.violations();
    }

    private static final class Group {
        final int[] resources;
        final List<Integer> tasks = new ArrayList<>();
        final Set<String> roles = new LinkedHashSet<>();
        long demand;

        Group(int[] resources) {
            this.resources = resources;
        }
    }

    private static final class Network {
        private final List<Group> groups;
        private final long[] supply;

        private Network(List<Group> groups, long[] supply) {
            this.groups = groups;
            this.supply = supply;
        }

        static Network build(CandidateIndexView index, PreSolveAnalyzer.Structure structure, Task[] tasks,
                             Set<Integer> skipped) {
            Map<String, Group> byResources = new LinkedHashMap<>();
            int resourceCount = index.resourceCount();
            long[][] intervals = new long[resourceCount][];
            int[] intervalCount = new int[resourceCount];
            long[] maxAmount = new long[resourceCount];
            long totalIntervals = 0;
            for (int t = 0; t < index.taskCount(); t++) {
                Task task = tasks[t];
                if (task == null || !task.requiredTask() || skipped.contains(t)) {
                    continue;
                }
                int[] need = structure.requirementNeeded(t);
                List<ResourceRequirement> requirements = task.effectiveRequirements();
                for (int q = 0; q < need.length; q++) {
                    int[] candidates = structure.requirementCandidateIds(t, q);
                    if (candidates == null) {
                        return null;
                    }
                    if (need[q] <= 0 || candidates.length < need[q]) {
                        continue;
                    }
                    long weight = Long.MAX_VALUE;
                    for (int c : candidates) {
                        weight = Math.min(weight, (long) index.candidateDuration(c) * amount(index, c));
                    }
                    if (weight <= 0) {
                        continue;
                    }
                    int[] resources = new int[candidates.length];
                    for (int i = 0; i < candidates.length; i++) {
                        int c = candidates[i];
                        int r = index.candidateResource(c);
                        resources[i] = r;
                        totalIntervals += addIntervals(index, structure, t, c, r, intervals, intervalCount);
                        if (totalIntervals > MAX_INTERVALS) {
                            return null;
                        }
                        maxAmount[r] = Math.max(maxAmount[r], amount(index, c));
                    }
                    int[] distinct = Arrays.stream(resources).distinct().sorted().toArray();
                    Group group = byResources.computeIfAbsent(Arrays.toString(distinct), key -> new Group(distinct));
                    group.demand += (long) need[q] * weight;
                    if (group.tasks.isEmpty() || group.tasks.get(group.tasks.size() - 1) != t) {
                        group.tasks.add(t);
                    }
                    if (q < requirements.size()) {
                        group.roles.add(requirements.get(q).role());
                    }
                }
            }
            if (byResources.isEmpty()) {
                return null;
            }
            long[] supply = new long[resourceCount];
            for (int r = 0; r < resourceCount; r++) {
                if (intervals[r] != null) {
                    supply[r] = supplyOf(index, structure, r, intervals[r], intervalCount[r], maxAmount[r]);
                }
            }
            return new Network(new ArrayList<>(byResources.values()), supply);
        }

        private static long amount(CandidateIndexView index, int candidate) {
            long capacity = Math.max(1, index.capacity(index.candidateResource(candidate)));
            return Math.min(Math.max(1, index.candidateAmount(candidate)), capacity);
        }

        private static int addIntervals(CandidateIndexView index, PreSolveAnalyzer.Structure structure, int task,
                                        int candidate, int resource, long[][] intervals, int[] count) {
            int[] runs = structure.startRuns(candidate);
            if (runs == null) {
                runs = index.estStart(task) <= index.lstStart(task)
                        ? new int[]{index.estStart(task), index.lstStart(task)} : new int[0];
            }
            long duration = index.candidateDuration(candidate);
            int added = 0;
            for (int i = 0; i + 1 < runs.length; i += 2) {
                long from = Math.max(0, runs[i]);
                long to = runs[i + 1] + duration;
                if (to <= from) {
                    continue;
                }
                if (intervals[resource] == null) {
                    intervals[resource] = new long[16];
                } else if (count[resource] == intervals[resource].length) {
                    intervals[resource] = Arrays.copyOf(intervals[resource], count[resource] * 2);
                }
                intervals[resource][count[resource]++] = (from << 32) | to;
                added++;
            }
            return added;
        }

        private static long supplyOf(CandidateIndexView index, PreSolveAnalyzer.Structure structure, int resource,
                                     long[] encoded, int count, long maxAmount) {
            Arrays.sort(encoded, 0, count);
            long covered = 0;
            long lo = Long.MAX_VALUE;
            long hi = Long.MIN_VALUE;
            long currentFrom = 0;
            long currentTo = -1;
            for (int i = 0; i < count; i++) {
                long from = encoded[i] >>> 32;
                long to = encoded[i] & 0xFFFFFFFFL;
                lo = Math.min(lo, from);
                hi = Math.max(hi, to);
                if (currentTo < 0 || from > currentTo) {
                    if (currentTo >= 0) {
                        covered += currentTo - currentFrom;
                    }
                    currentFrom = from;
                    currentTo = to;
                } else if (to > currentTo) {
                    currentTo = to;
                }
            }
            if (currentTo >= 0) {
                covered += currentTo - currentFrom;
            }
            long ticks = Math.min(covered, structure.availableTicks(resource, (int) lo, (int) hi));
            long supply = Math.max(1, index.capacity(resource)) * ticks;
            int maxLoad = index.maxLoadTicks(resource);
            if (maxLoad != Integer.MAX_VALUE) {
                supply = Math.min(supply, (long) maxLoad * Math.max(1, maxAmount));
            }
            return supply;
        }

        List<Violation> violations() {
            int groupCount = groups.size();
            int firstGroup = 2;
            int firstResource = firstGroup + groupCount;
            Flow flow = new Flow(firstResource + supply.length);
            long totalDemand = 0;
            boolean[] used = new boolean[supply.length];
            for (int g = 0; g < groupCount; g++) {
                Group group = groups.get(g);
                flow.edge(0, firstGroup + g, group.demand);
                totalDemand += group.demand;
                for (int r : group.resources) {
                    flow.edge(firstGroup + g, firstResource + r, INFINITE);
                    used[r] = true;
                }
            }
            for (int r = 0; r < supply.length; r++) {
                if (used[r]) {
                    flow.edge(firstResource + r, 1, supply[r]);
                }
            }
            if (flow.maxFlow(0, 1) >= totalDemand) {
                return List.of();
            }
            return components(flow.reachable(0), firstGroup);
        }

        private List<Violation> components(boolean[] reachable, int firstGroup) {
            int groupCount = groups.size();
            int[] parent = new int[groupCount];
            for (int g = 0; g < groupCount; g++) {
                parent[g] = g;
            }
            Map<Integer, Integer> owner = new HashMap<>();
            for (int g = 0; g < groupCount; g++) {
                if (!reachable[firstGroup + g]) {
                    continue;
                }
                for (int r : groups.get(g).resources) {
                    Integer other = owner.putIfAbsent(r, g);
                    if (other != null) {
                        parent[find(parent, g)] = find(parent, other);
                    }
                }
            }
            Map<Integer, List<Integer>> members = new LinkedHashMap<>();
            for (int g = 0; g < groupCount; g++) {
                if (reachable[firstGroup + g]) {
                    members.computeIfAbsent(find(parent, g), key -> new ArrayList<>()).add(g);
                }
            }
            List<Violation> out = new ArrayList<>();
            for (List<Integer> component : members.values()) {
                long demand = 0;
                Set<Integer> resources = new TreeSet<>();
                Set<Integer> taskSet = new TreeSet<>();
                Set<String> roles = new LinkedHashSet<>();
                for (int g : component) {
                    Group group = groups.get(g);
                    demand += group.demand;
                    taskSet.addAll(group.tasks);
                    roles.addAll(group.roles);
                    for (int r : group.resources) {
                        resources.add(r);
                    }
                }
                long offered = 0;
                for (int r : resources) {
                    offered += supply[r];
                }
                if (demand > offered) {
                    out.add(new Violation(resources.stream().mapToInt(Integer::intValue).toArray(),
                            taskSet.stream().mapToInt(Integer::intValue).toArray(), roles, demand, offered));
                }
            }
            out.sort(Comparator.comparingLong(Violation::deficit).reversed());
            return out;
        }

        private static int find(int[] parent, int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }
    }

    /**
     * Maximum flow (Dinic) with {@code long} capacities.
     */
    private static final class Flow {
        private final int[] head;
        private final int[] level;
        private final int[] cursor;
        private int[] to = new int[64];
        private long[] cap = new long[64];
        private int[] next = new int[64];
        private int edges;

        Flow(int nodes) {
            head = new int[nodes];
            level = new int[nodes];
            cursor = new int[nodes];
            Arrays.fill(head, -1);
        }

        void edge(int from, int target, long capacity) {
            add(from, target, capacity);
            add(target, from, 0);
        }

        private void add(int from, int target, long capacity) {
            if (edges == to.length) {
                to = Arrays.copyOf(to, edges * 2);
                cap = Arrays.copyOf(cap, edges * 2);
                next = Arrays.copyOf(next, edges * 2);
            }
            to[edges] = target;
            cap[edges] = capacity;
            next[edges] = head[from];
            head[from] = edges++;
        }

        long maxFlow(int source, int sink) {
            long total = 0;
            while (levels(source, sink)) {
                System.arraycopy(head, 0, cursor, 0, head.length);
                long pushed;
                while ((pushed = push(source, sink, INFINITE)) > 0) {
                    total += pushed;
                }
            }
            return total;
        }

        private boolean levels(int source, int sink) {
            Arrays.fill(level, -1);
            level[source] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            while (!queue.isEmpty()) {
                int node = queue.poll();
                for (int e = head[node]; e >= 0; e = next[e]) {
                    if (cap[e] > 0 && level[to[e]] < 0) {
                        level[to[e]] = level[node] + 1;
                        queue.add(to[e]);
                    }
                }
            }
            return level[sink] >= 0;
        }

        private long push(int node, int sink, long limit) {
            if (node == sink) {
                return limit;
            }
            for (; cursor[node] >= 0; cursor[node] = next[cursor[node]]) {
                int e = cursor[node];
                if (cap[e] > 0 && level[to[e]] == level[node] + 1) {
                    long pushed = push(to[e], sink, Math.min(limit, cap[e]));
                    if (pushed > 0) {
                        cap[e] -= pushed;
                        cap[e ^ 1] += pushed;
                        return pushed;
                    }
                }
            }
            return 0;
        }

        boolean[] reachable(int source) {
            boolean[] seen = new boolean[head.length];
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            seen[source] = true;
            queue.add(source);
            while (!queue.isEmpty()) {
                int node = queue.poll();
                for (int e = head[node]; e >= 0; e = next[e]) {
                    if (cap[e] > 0 && !seen[to[e]]) {
                        seen[to[e]] = true;
                        queue.add(to[e]);
                    }
                }
            }
            return seen;
        }
    }
}

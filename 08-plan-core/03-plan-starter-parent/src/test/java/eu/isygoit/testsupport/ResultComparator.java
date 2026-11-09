package eu.isygoit.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.ObjectiveScore;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test helpers to compare and inspect {@link SolveResult}s: determinism comparison (G13) and the structural facts the golden
 * scenarios assert (makespan, tier values, resources per task, overlap and load sweeps).
 *
 * <p>Contract: {@link #assertSameIgnoringMetrics} compares the JSON form of two results with the whole {@code metrics}
 * object removed (time fields and search counters differ between runs; design 8.3 and 9.5 G13) and reports the FIRST
 * differing JSON path. Everything else (status, solution, relaxations, diagnostics) must be identical. The inspection helpers
 * never mutate the result and never throw for missing data: they return empty collections, {@link Optional#empty()} or
 * {@code -1}.
 *
 * <p>Thread-safety: stateless; the shared {@link ObjectMapper} is configured once and is thread-safe.
 *
 * <p>Units: ticks unless a method name says otherwise (tier values are the exact scaled integers of
 * {@link ObjectiveScore#rawValue()}).
 */
public final class ResultComparator {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ResultComparator() {
    }

    // ================================================================== determinism comparison

    /** JSON tree of the result without the {@code metrics} object. */
    public static JsonNode withoutMetrics(SolveResult result) {
        ObjectNode node = MAPPER.valueToTree(result);
        node.remove("metrics");
        return node;
    }

    /** True when both results are JSON-equal ignoring {@code metrics}. */
    public static boolean sameIgnoringMetrics(SolveResult a, SolveResult b) {
        return withoutMetrics(a).equals(withoutMetrics(b));
    }

    /** Fails with the first differing JSON path when the results differ (ignoring {@code metrics}). */
    public static void assertSameIgnoringMetrics(SolveResult expected, SolveResult actual) {
        String difference = firstDifference("$", withoutMetrics(expected), withoutMetrics(actual));
        if (difference != null) {
            fail("Results differ (ignoring metrics) at " + difference);
        }
    }

    /** Fails when the assignments (task, start, end, slot, resources) differ; ignores everything else. */
    public static void assertSameAssignments(SolveResult expected, SolveResult actual) {
        JsonNode a = MAPPER.valueToTree(expected.solution() == null ? List.of() : expected.solution().assignments());
        JsonNode b = MAPPER.valueToTree(actual.solution() == null ? List.of() : actual.solution().assignments());
        String difference = firstDifference("$.solution.assignments", a, b);
        if (difference != null) {
            fail("Assignments differ at " + difference);
        }
    }

    private static String firstDifference(String path, JsonNode a, JsonNode b) {
        if (a.equals(b)) {
            return null;
        }
        if (a.isObject() && b.isObject()) {
            Set<String> names = new TreeSet<>();
            a.fieldNames().forEachRemaining(names::add);
            b.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode x = a.get(name);
                JsonNode y = b.get(name);
                if (x == null || y == null) {
                    return path + "." + name + " (present in " + (x == null ? "actual only" : "expected only") + ")";
                }
                String nested = firstDifference(path + "." + name, x, y);
                if (nested != null) {
                    return nested;
                }
            }
        }
        if (a.isArray() && b.isArray()) {
            int common = Math.min(a.size(), b.size());
            for (int i = 0; i < common; i++) {
                String nested = firstDifference(path + "[" + i + "]", a.get(i), b.get(i));
                if (nested != null) {
                    return nested;
                }
            }
            if (a.size() != b.size()) {
                return path + " (length " + a.size() + " vs " + b.size() + ")";
            }
        }
        return path + ": expected " + abbreviate(a) + " but was " + abbreviate(b);
    }

    private static String abbreviate(JsonNode node) {
        String text = node.toString();
        return text.length() > 160 ? text.substring(0, 157) + "..." : text;
    }

    // ================================================================== inspection helpers

    /** Assignments by task id in result order; empty when there is no solution. */
    public static Map<String, Assignment> assignmentsByTask(SolveResult result) {
        Map<String, Assignment> byTask = new LinkedHashMap<>();
        if (result != null && result.solution() != null) {
            for (Assignment a : result.solution().assignments()) {
                byTask.put(a.taskId(), a);
            }
        }
        return byTask;
    }

    /** Ids of the tasks that are assigned, as a sorted set. */
    public static Set<String> assignedTaskIds(SolveResult result) {
        return new TreeSet<>(assignmentsByTask(result).keySet());
    }

    /** Ids of the unassigned (unselected optional) tasks, as a sorted set. */
    public static Set<String> unassignedTaskIds(SolveResult result) {
        if (result == null || result.solution() == null) {
            return Set.of();
        }
        return result.solution().unassignedTasks().stream()
                .map(u -> u.taskId()).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Resource ids used by an assignment, in assignment order. */
    public static List<String> resourceIds(Assignment assignment) {
        return assignment.resources().stream().map(ResourceAssignment::resourceId).collect(Collectors.toList());
    }

    /** Resource ids of the task (empty when the task is not assigned). */
    public static List<String> resourceIds(SolveResult result, String taskId) {
        Assignment a = assignmentsByTask(result).get(taskId);
        return a == null ? List.of() : resourceIds(a);
    }

    /** Largest {@code endTick} over all assignments (the makespan), or -1 without assignments. */
    public static int makespan(SolveResult result) {
        return assignmentsByTask(result).values().stream().mapToInt(Assignment::endTick).max().orElse(-1);
    }

    /** Number of assignments carrying the given slot id. */
    public static long countInSlot(SolveResult result, String slotId) {
        return assignmentsByTask(result).values().stream().filter(a -> slotId.equals(a.slotId())).count();
    }

    /** Sum of {@link ObjectiveScore#rawValue()} of the objectives of the tier (empty when none is reported). */
    public static Optional<Long> tierRawValue(SolveResult result, int tier) {
        List<ObjectiveScore> scores = objectiveScores(result, tier);
        if (scores.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(scores.stream().mapToLong(ObjectiveScore::rawValue).sum());
    }

    /** Sum of {@link ObjectiveScore#score()} of the objectives of the tier (human value), empty when none is reported. */
    public static Optional<Double> tierScore(SolveResult result, int tier) {
        List<ObjectiveScore> scores = objectiveScores(result, tier);
        if (scores.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(scores.stream().mapToDouble(ObjectiveScore::score).sum());
    }

    /** The per-objective scores of one tier (design 8.2, evaluated by the independent validator). */
    public static List<ObjectiveScore> objectiveScores(SolveResult result, int tier) {
        if (result == null || result.solution() == null) {
            return List.of();
        }
        return result.solution().qualityDetails().stream().filter(q -> q.tier() == tier).collect(Collectors.toList());
    }

    /** The engine-reported value of a tier ({@code Metrics.tiers}), e.g. tier -2 = relaxation cost (G5: 1000). */
    public static Optional<TierValue> metricsTier(SolveResult result, int tier) {
        if (result == null || result.metrics() == null) {
            return Optional.empty();
        }
        return result.metrics().tiers().stream().filter(t -> t.tier() == tier).findFirst();
    }

    // ================================================================== structural sweeps (golden facts)

    /**
     * Fails when two assignments overlap on a resource whose capacity is {@code 1} in the request (the "machines never
     * overlap" fact of G8). Intervals are half-open {@code [startTick, endTick)}.
     */
    public static void assertNoOverlapOnUnaryResources(PlanningRequest request, SolveResult result) {
        Set<String> unary = request.resources().stream()
                .filter(r -> r.capacity() == null || r.capacity() == 1)
                .map(r -> r.id()).collect(Collectors.toSet());
        Map<String, List<Assignment>> perResource = new LinkedHashMap<>();
        for (Assignment a : assignmentsByTask(result).values()) {
            for (ResourceAssignment ra : a.resources()) {
                if (unary.contains(ra.resourceId())) {
                    perResource.computeIfAbsent(ra.resourceId(), k -> new ArrayList<>()).add(a);
                }
            }
        }
        for (Map.Entry<String, List<Assignment>> entry : perResource.entrySet()) {
            List<Assignment> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator.comparingInt(Assignment::startTick).thenComparingInt(Assignment::endTick));
            for (int i = 1; i < sorted.size(); i++) {
                Assignment previous = sorted.get(i - 1);
                Assignment current = sorted.get(i);
                if (current.startTick() < previous.endTick() && current.endTick() > current.startTick()
                        && previous.endTick() > previous.startTick()) {
                    fail("Tasks " + previous.taskId() + " [" + previous.startTick() + "," + previous.endTick() + ") and "
                            + current.taskId() + " [" + current.startTick() + "," + current.endTick()
                            + ") overlap on resource " + entry.getKey());
                }
            }
        }
    }

    /** Peak concurrent consumed amount on a resource (sweep line over start/end events); 0 when it is not used. */
    public static int peakLoad(SolveResult result, String resourceId) {
        List<int[]> events = new ArrayList<>();   // {tick, delta}; ends sort before starts at the same tick
        for (Assignment a : assignmentsByTask(result).values()) {
            if (a.endTick() <= a.startTick()) {
                continue;
            }
            for (ResourceAssignment ra : a.resources()) {
                if (resourceId.equals(ra.resourceId())) {
                    events.add(new int[]{a.startTick(), ra.amount()});
                    events.add(new int[]{a.endTick(), -ra.amount()});
                }
            }
        }
        events.sort(Comparator.<int[]>comparingInt(e -> e[0]).thenComparingInt(e -> e[1]));
        int load = 0;
        int peak = 0;
        for (int[] event : events) {
            load += event[1];
            peak = Math.max(peak, load);
        }
        return peak;
    }

    /**
     * Fails when {@code successor} does not respect FINISH_START with {@code minLag} after {@code predecessor}
     * (both must be assigned): {@code start(successor) >= end(predecessor) + minLag}.
     */
    public static void assertFinishStart(SolveResult result, String predecessor, String successor, int minLag) {
        Map<String, Assignment> byTask = assignmentsByTask(result);
        Assignment p = byTask.get(predecessor);
        Assignment s = byTask.get(successor);
        if (p == null || s == null) {
            fail("Both " + predecessor + " and " + successor + " must be assigned");
            return;
        }
        if (s.startTick() < p.endTick() + minLag) {
            fail(successor + " starts at " + s.startTick() + " but " + predecessor + " ends at " + p.endTick()
                    + " (minLag " + minLag + ")");
        }
    }

    /** Fails when an assigned required task ends after its hard deadline (ticks computed from the request horizon). */
    public static void assertDeadlinesRespected(PlanningRequest request, SolveResult result) {
        Map<String, Assignment> byTask = assignmentsByTask(result);
        for (Task task : request.tasks()) {
            Assignment a = byTask.get(task.id());
            if (a != null && task.deadline() != null && a.end().isAfter(task.deadline())) {
                fail("Task " + task.id() + " ends at " + a.end() + " after its deadline " + task.deadline());
            }
        }
    }
}

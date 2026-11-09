package eu.isygoit.solver.spi;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;

/**
 * Read-only, engine-neutral view of the int-indexed {@code CandidateIndex} (design 5.2 / 5.4), handed to families for
 * estimation, validation and checking.
 * <p>
 * Implemented by {@code eu.isygoit.solver.index.CandidateIndex} (WP2, which MUST override every method below) and by the
 * LIGHT view that validation uses before the index exists. Immutable after construction, thus thread-safe. Indices are
 * request order; times are integer ticks (tick 0 = horizon start).
 * <p>
 * Guaranteed in the light view: counts of tasks/resources/slots, ids, id-to-index lookups, {@link #horizon()},
 * {@link #limits()}, scope resolution. Methods with a default body (per-task / per-resource / per-candidate data)
 * throw {@link UnsupportedOperationException} on a light view; {@code CandidateIndex} overrides them all.
 */
public interface CandidateIndexView {

    // ------------------------------------------------------------------ sizes, horizon, limits (always available)

    /** @return number of tasks */
    int taskCount();

    /** @return number of resources */
    int resourceCount();

    /** @return number of slots */
    int slotCount();

    /** @return the problem horizon */
    Horizon horizon();

    /** @return the horizon length in ticks */
    int horizonTicks();

    /** @return the configured safety limits */
    PlanLimitsProperties limits();

    // ------------------------------------------------------------------ ids and lookups (always available)

    /** @return the id of task index {@code t} */
    String taskId(int t);

    /** @return the id of resource index {@code r} */
    String resourceId(int r);

    /** @return the id of slot index {@code s} */
    String slotId(int s);

    /** @return the task index of an id, or {@code -1} when unknown (build-time map lookup, not for hot loops) */
    int taskIdx(String id);

    /** @return the resource index of an id, or {@code -1} when unknown */
    int resourceIdx(String id);

    /** @return the slot index of an id, or {@code -1} when unknown */
    int slotIdx(String id);

    /** @return the task indices selected by the scope, ascending, cached; callers must not modify the array */
    int[] resolveTasks(ConstraintScope scope);

    /** @return the resource indices selected by the scope, ascending, cached; callers must not modify the array */
    int[] resolveResources(ConstraintScope scope);

    /** @return task index groups, one per task tag starting with {@code prefix}, ordered by tag; ascending inside a group */
    int[][] groupTasksByTagPrefix(String prefix);

    // ------------------------------------------------------------------ per-task data (index only)

    /** @return number of surviving candidates (task, requirement, resource) */
    default int candidateCount() {
        throw new UnsupportedOperationException("candidateCount");
    }

    /** @return base duration of the task in ticks */
    default int duration(int task) {
        throw new UnsupportedOperationException("duration");
    }

    /** @return capacity amount the task consumes by default (demand, at least 1) */
    default int demand(int task) {
        throw new UnsupportedOperationException("demand");
    }

    /** @return task priority (at least 0) */
    default int priority(int task) {
        throw new UnsupportedOperationException("priority");
    }

    /** @return {@code true} for a required task, {@code false} for an optional one */
    default boolean required(int task) {
        throw new UnsupportedOperationException("required");
    }

    /** @return effective prize (prize, default 1; 0 for required tasks) */
    default long prize(int task) {
        throw new UnsupportedOperationException("prize");
    }

    /** @return earliest start tick (release) */
    default int release(int task) {
        throw new UnsupportedOperationException("release");
    }

    /** @return latest END tick: min(deadline, horizonTicks) */
    default int latestEnd(int task) {
        throw new UnsupportedOperationException("latestEnd");
    }

    /** @return the due tick, or {@code Integer.MIN_VALUE} when the task has no due date */
    default int due(int task) {
        throw new UnsupportedOperationException("due");
    }

    /** @return earliest start tick after domain pruning */
    default int estStart(int task) {
        throw new UnsupportedOperationException("estStart");
    }

    /** @return latest start tick after domain pruning */
    default int lstStart(int task) {
        throw new UnsupportedOperationException("lstStart");
    }

    /** @return the pinned start tick, or {@code -1} */
    default int pinStart(int task) {
        throw new UnsupportedOperationException("pinStart");
    }

    // ------------------------------------------------------------------ per-resource data (index only)

    /** @return cumulative capacity of the resource (at least 1) */
    default int capacity(int resource) {
        throw new UnsupportedOperationException("capacity");
    }

    /** @return the horizon-wide load cap in ticks, or {@code Integer.MAX_VALUE} */
    default int maxLoadTicks(int resource) {
        throw new UnsupportedOperationException("maxLoadTicks");
    }

    // ------------------------------------------------------------------ candidates (index only)

    /** @return first candidate id of the task (candidates of a task are the range [candidateBegin, candidateEnd)) */
    default int candidateBegin(int task) {
        throw new UnsupportedOperationException("candidateBegin");
    }

    /** @return one past the last candidate id of the task */
    default int candidateEnd(int task) {
        throw new UnsupportedOperationException("candidateEnd");
    }

    /** @return the task index of a candidate */
    default int candidateTask(int candidate) {
        throw new UnsupportedOperationException("candidateTask");
    }

    /** @return the resource index of a candidate */
    default int candidateResource(int candidate) {
        throw new UnsupportedOperationException("candidateResource");
    }

    /** @return the duration in ticks the task has on the candidate's resource */
    default int candidateDuration(int candidate) {
        throw new UnsupportedOperationException("candidateDuration");
    }

    /** @return the capacity amount consumed on the candidate's resource */
    default int candidateAmount(int candidate) {
        throw new UnsupportedOperationException("candidateAmount");
    }
}

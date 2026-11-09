package eu.isygoit.dto.request.params;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Params of {@code forbidden_pair}. TASK_TASK: two tasks never on the same resource (SAME_RESOURCE) or never
 * overlapping (TIME_OVERLAP); RESOURCE_RESOURCE: two resources never together on one task ({@code relation} is
 * ignored). {@code kind} and {@code relation} (TASK_TASK) are mandatory, checked by the family validator.
 * <pre>{@code
 * { "kind": "TASK_TASK", "relation": "SAME_RESOURCE", "pairs": [{ "a": "t1", "b": "t2" }] }
 * }</pre>
 *
 * @param kind     what the pairs identify
 * @param relation TASK_TASK relation
 * @param pairs    forbidden id pairs
 */
public record ForbiddenPairParams(Kind kind, Relation relation, List<IdPair> pairs) {

    /**
     * What the pairs identify.
     */
    public enum Kind { TASK_TASK, RESOURCE_RESOURCE }

    /**
     * Relation forbidden between two tasks (TASK_TASK only).
     */
    public enum Relation { SAME_RESOURCE, TIME_OVERLAP }

    /**
     * An unordered pair of ids.
     *
     * @param a first id
     * @param b second id
     */
    public record IdPair(String a, String b) {
    }

    public ForbiddenPairParams {
        pairs = CollectionCopies.list(pairs);
    }
}

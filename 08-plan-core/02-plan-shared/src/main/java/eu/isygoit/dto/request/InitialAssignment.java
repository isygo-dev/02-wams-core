package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.time.Instant;
import java.util.List;

/**
 * One assignment of a previous plan. {@code locked=false}: warm-start hint only. {@code locked=true}: frozen (the
 * task keeps exactly these values; the problem is infeasible if they violate hard constraints).
 * <pre>{@code
 * { "taskId": "A1", "start": "2026-09-07T08:00:00Z", "resourceIds": ["M1"], "locked": true }
 * }</pre>
 *
 * @param taskId      the task
 * @param start       start instant (tick aligned)
 * @param resourceIds resources in requirement order, empty = start only
 * @param locked      frozen when true, hint when false
 */
public record InitialAssignment(String taskId, Instant start, List<String> resourceIds, boolean locked) {

    public InitialAssignment {
        resourceIds = CollectionCopies.list(resourceIds);
    }
}

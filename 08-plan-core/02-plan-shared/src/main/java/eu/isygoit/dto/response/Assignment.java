package eu.isygoit.dto.response;

import eu.isygoit.util.CollectionCopies;

import java.time.Instant;
import java.util.List;

/**
 * One scheduled task. Ticks are relative to {@code horizon.start}; the instants are
 * {@code horizon.toInstant(tick)}.
 * <pre>{@code
 * { "taskId": "A1", "optional": false, "startTick": 20, "endTick": 50,
 *   "start": "2026-09-07T08:20:00Z", "end": "2026-09-07T08:50:00Z", "slotId": null,
 *   "resources": [{ "resourceId": "M1", "role": "machine", "amount": 1 }] }
 * }</pre>
 *
 * @param taskId    the task
 * @param optional  true = an optional task that WAS selected
 * @param startTick start tick
 * @param endTick   {@code startTick + duration actually used} (resource-specific duration)
 * @param start     start instant
 * @param end       end instant
 * @param slotId    slot-mode anchor, else null
 * @param resources assigned resources in requirement order, then resource id
 */
public record Assignment(
        String taskId,
        boolean optional,
        int startTick,
        int endTick,
        Instant start,
        Instant end,
        String slotId,
        List<ResourceAssignment> resources) {

    public Assignment {
        resources = CollectionCopies.list(resources);
    }
}

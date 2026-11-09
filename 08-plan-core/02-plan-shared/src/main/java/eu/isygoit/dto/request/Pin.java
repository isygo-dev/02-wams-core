package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.time.Instant;
import java.util.List;

/**
 * Definition-level hard fact: "this task always starts at {@code start} and/or is always done by
 * {@code resourceIds}". Either part may be absent ({@code start} null, {@code resourceIds} empty).
 * {@code resourceIds} are matched to the task requirements in order of appearance (one id per required slot of the
 * task, or fewer for a partial pin; checked by the validator).
 * <pre>{@code
 * { "start": "2026-09-07T08:00:00Z", "resourceIds": ["press-1"] }
 * }</pre>
 *
 * @param start       pinned start instant (tick aligned) or null
 * @param resourceIds pinned resource ids, empty = no resource pin
 */
public record Pin(Instant start, List<String> resourceIds) {

    public Pin {
        resourceIds = CollectionCopies.list(resourceIds);
    }
}

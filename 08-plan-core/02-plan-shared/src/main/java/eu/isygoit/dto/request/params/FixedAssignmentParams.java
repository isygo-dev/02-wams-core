package eu.isygoit.dto.request.params;

import eu.isygoit.util.CollectionCopies;

import java.time.Instant;
import java.util.List;

/**
 * Params of {@code fixed_assignment}: scoped tasks take the given start and/or resources (bulk, soft or relaxable
 * form of {@code Task.pin}).
 * <pre>{@code
 * { "start": "2026-09-07T08:00:00Z", "resourceIds": ["press-1"], "role": "machine" }
 * }</pre>
 *
 * @param start       fixed start (tick aligned) or null
 * @param resourceIds fixed resources, empty = none
 * @param role        requirement role the resource pin applies to, null = all
 */
public record FixedAssignmentParams(Instant start, List<String> resourceIds, String role) {

    public FixedAssignmentParams {
        resourceIds = CollectionCopies.list(resourceIds);
    }
}

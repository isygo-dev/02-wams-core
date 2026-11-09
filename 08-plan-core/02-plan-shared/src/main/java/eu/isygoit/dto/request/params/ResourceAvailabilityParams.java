package eu.isygoit.dto.request.params;

import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Params of {@code resource_availability}: extra unavailable intervals, or "available only" windows, for the
 * scoped resources; as a SOFT constraint it models overtime.
 * <pre>{@code
 * { "unavailable": [{ "start": "2026-09-07T12:00:00Z", "end": "2026-09-07T13:00:00Z" }] }
 * }</pre>
 *
 * @param unavailable   blocked intervals
 * @param availableOnly when non-empty, the resource is available ONLY inside these windows
 */
public record ResourceAvailabilityParams(List<TimeWindow> unavailable, List<TimeWindow> availableOnly) {

    public ResourceAvailabilityParams {
        unavailable = CollectionCopies.list(unavailable);
        availableOnly = CollectionCopies.list(availableOnly);
    }
}

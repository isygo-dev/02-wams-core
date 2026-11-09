package eu.isygoit.dto.request.params;

import eu.isygoit.dto.request.TimeWindow;

/**
 * Params of {@code resource_capacity}: lowers the capacity of the scoped resources to
 * {@code min(Resource.capacity, capacity)} inside {@code window} (default: the whole horizon). {@code capacity} is
 * {@code >= 0}; for a unary resource only 0 is meaningful.
 * <pre>{@code
 * { "capacity": 1, "window": { "start": "2026-09-07T12:00:00Z", "end": "2026-09-07T14:00:00Z" } }
 * }</pre>
 *
 * @param capacity reduced capacity
 * @param window   interval of the reduction, null = horizon
 */
public record ResourceCapacityParams(Integer capacity, TimeWindow window) {
}

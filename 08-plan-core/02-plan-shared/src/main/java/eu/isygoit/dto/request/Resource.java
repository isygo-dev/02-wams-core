package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;
import eu.isygoit.util.ImmutableJsonMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Anything a task consumes or occupies (machine, person, room, crane pool). A resource with capacity 1 is unary
 * (implicit no-overlap); capacity {@code > 1} is cumulative (sum of the demands of overlapping tasks).
 * <pre>{@code
 * { "id": "cleo", "typeId": "nurse", "skills": ["nurse"],
 *   "unavailable": [{ "start": "2026-09-07T08:00:00Z", "end": "2026-09-07T16:00:00Z" }] }
 * }</pre>
 * Defaults applied here: {@code capacity} = 1, {@code costPerTick} = 0, {@code costPerUse} = 0, empty
 * skills/tags/unavailable/attributes.
 *
 * @param id            unique resource id
 * @param typeId        optional, validated against {@code domain.resourceTypes} when a domain is given
 * @param name          display only
 * @param capacity      cumulative capacity {@code >= 1}, default 1 (1 means unary)
 * @param skills        capabilities matched by {@link ResourceSelector#skills()}
 * @param tags          free labels matched by selectors and by resource-oriented constraint scopes
 * @param calendarId    working calendar, null = always available
 * @param unavailable   extra blocked intervals (instants)
 * @param costPerTick   {@code >= 0}, default 0, read by minimize_resource_cost (decimal: scaled)
 * @param costPerUse    {@code >= 0}, default 0, paid once if the resource is used at all (decimal: scaled)
 * @param maxLoadTicks  optional horizon-wide cap on the sum of assigned task durations (built-in hard cap)
 * @param setupMatrixId null = no setup times on this resource
 * @param attributes    opaque extension map, never read by the engine
 */
public record Resource(
        String id,
        String typeId,
        String name,
        Integer capacity,
        Set<String> skills,
        Set<String> tags,
        String calendarId,
        List<TimeWindow> unavailable,
        Double costPerTick,
        Double costPerUse,
        Integer maxLoadTicks,
        String setupMatrixId,
        Map<String, Object> attributes) {

    public Resource {
        capacity = capacity == null ? Integer.valueOf(1) : capacity;
        skills = CollectionCopies.orderedSet(skills);
        tags = CollectionCopies.orderedSet(tags);
        unavailable = CollectionCopies.list(unavailable);
        costPerTick = costPerTick == null ? Double.valueOf(0.0) : costPerTick;
        costPerUse = costPerUse == null ? Double.valueOf(0.0) : costPerUse;
        attributes = ImmutableJsonMap.copyOf(attributes);
    }
}

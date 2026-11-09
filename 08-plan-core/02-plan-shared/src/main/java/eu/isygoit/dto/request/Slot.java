package eu.isygoit.dto.request;

import eu.isygoit.util.ImmutableJsonMap;

import java.time.Instant;
import java.util.Map;

/**
 * A named, typed, priced time interval: the start anchor of slot mode (design D-2). A task with
 * {@code startSlots} must START at the start of one of the listed slots and END at or before the slot end. A slot is
 * NOT a capacity container: capacity is by resource over time.
 * <pre>{@code
 * { "id": "mon-p1", "typeId": "period", "label": "Mon 08:00",
 *   "start": "2026-09-07T06:00:00Z", "end": "2026-09-07T07:00:00Z", "cost": 0.0 }
 * }</pre>
 *
 * @param id         unique slot id
 * @param typeId     optional type id, validated against {@code domain.slotTypes} when a domain is given
 * @param label      display only
 * @param start      slot start (instant, tick aligned)
 * @param end        slot end (instant, tick aligned, after start)
 * @param cost       price of using the slot, read by {@code minimize_slot_cost} (scaled by the objective scale)
 * @param attributes opaque extension map, never read by the engine
 */
public record Slot(
        String id,
        String typeId,
        String label,
        Instant start,
        Instant end,
        double cost,
        Map<String, Object> attributes) {

    public Slot {
        attributes = ImmutableJsonMap.copyOf(attributes);
    }
}

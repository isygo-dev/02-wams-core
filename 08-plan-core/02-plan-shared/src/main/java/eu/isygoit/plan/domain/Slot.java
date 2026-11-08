package eu.isygoit.plan.domain;

import java.time.Instant;
import java.util.Map;

public record Slot(String id, String typeId, Instant start, Instant end, Map<String, Object> attributes) {
    public Slot {
        attributes = ImmutableJsonMap.copyOf(attributes);
    }
}

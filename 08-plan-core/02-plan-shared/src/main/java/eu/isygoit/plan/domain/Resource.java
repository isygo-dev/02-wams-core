package eu.isygoit.plan.domain;

import java.util.List;
import java.util.Map;

public record Resource(String id, String typeId, Map<String, Object> attributes, List<String> unavailableSlots) {
    public Resource {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        unavailableSlots = unavailableSlots == null ? List.of() : List.copyOf(unavailableSlots);
    }
}

package eu.isygoit.plan.domain;

import java.time.Instant;
import java.util.List;

public record Assignment(
        String taskId,
        String slotId,
        List<ResourceAssignment> resources,
        Instant start,
        Instant end) {

    public Assignment {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}

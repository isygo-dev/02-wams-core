package eu.isygoit.plan.domain;

import java.util.Map;

public record Task(
        String id,
        String typeId,
        Map<String, Object> attributes,
        TaskCandidates candidates,
        Boolean required) {

    public Task {
        attributes = ImmutableJsonMap.copyOf(attributes);
        candidates = candidates == null ? new TaskCandidates(null, null, null) : candidates;
        required = required == null ? Boolean.TRUE : required;
    }

    public Task(String id, String typeId, Map<String, Object> attributes, TaskCandidates candidates) {
        this(id, typeId, attributes, candidates, true);
    }

    public boolean requiredTask() {
        return Boolean.TRUE.equals(required);
    }
}

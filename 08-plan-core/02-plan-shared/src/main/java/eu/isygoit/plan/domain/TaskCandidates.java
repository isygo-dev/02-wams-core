package eu.isygoit.plan.domain;

import java.util.List;

public record TaskCandidates(
        List<String> resourceSelectors,
        List<String> slotSelectors,
        List<String> capabilityTags) {

    public TaskCandidates {
        resourceSelectors = resourceSelectors == null ? List.of() : List.copyOf(resourceSelectors);
        slotSelectors = slotSelectors == null ? List.of() : List.copyOf(slotSelectors);
        capabilityTags = capabilityTags == null ? List.of() : List.copyOf(capabilityTags);
    }
}

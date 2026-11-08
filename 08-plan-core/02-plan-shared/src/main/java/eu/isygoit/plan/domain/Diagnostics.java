package eu.isygoit.plan.domain;

import java.util.List;
import java.util.Map;

public record Diagnostics(
        String message,
        List<String> infeasibilityCore,
        Map<String, Object> details) {

    public Diagnostics {
        infeasibilityCore = infeasibilityCore == null ? List.of() : List.copyOf(infeasibilityCore);
        details = ImmutableJsonMap.copyOf(details);
    }
}

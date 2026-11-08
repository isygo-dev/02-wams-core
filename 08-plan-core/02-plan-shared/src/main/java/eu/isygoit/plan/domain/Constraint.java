package eu.isygoit.plan.domain;

import eu.isygoit.enums.IEnumEnforcement;

import java.util.Map;

public record Constraint(
        String id,
        String type,
        IEnumEnforcement.Types enforcement,
        ConstraintScope scope,
        Map<String, Object> params,
        RelaxationPolicy relaxation,
        int weight) {

    public Constraint {
        params = ImmutableJsonMap.copyOf(params);
        if (weight < 0) {
            throw new IllegalArgumentException("Constraint weight must not be negative");
        }
    }
}

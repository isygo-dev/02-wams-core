package eu.isygoit.plan.domain;

public record Relaxation(
        String constraintId,
        Object originalValue,
        Object relaxedValue,
        double impactOnScore,
        boolean authorized) {
}

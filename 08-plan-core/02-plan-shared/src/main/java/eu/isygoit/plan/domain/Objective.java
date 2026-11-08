package eu.isygoit.plan.domain;

public record Objective(String id, String type, int weight) {
    public Objective {
        if (weight < 0) {
            throw new IllegalArgumentException("Objective weight must not be negative");
        }
    }
}

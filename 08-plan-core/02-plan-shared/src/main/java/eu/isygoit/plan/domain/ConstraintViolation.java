package eu.isygoit.plan.domain;

public record ConstraintViolation(
        String constraintId,
        String taskId,
        String message,
        double impact) {
}

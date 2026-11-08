package eu.isygoit.plan.domain;

import java.util.List;

public record PlanningSolution(
        List<Assignment> assignments,
        List<UnassignedTask> unassignedTasks,
        List<ConstraintViolation> hardViolations,
        List<ConstraintViolation> softViolations,
        List<ObjectiveScore> qualityDetails,
        double qualityScore) {

    public PlanningSolution {
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
        unassignedTasks = unassignedTasks == null ? List.of() : List.copyOf(unassignedTasks);
        hardViolations = hardViolations == null ? List.of() : List.copyOf(hardViolations);
        softViolations = softViolations == null ? List.of() : List.copyOf(softViolations);
        qualityDetails = qualityDetails == null ? List.of() : List.copyOf(qualityDetails);
        if (!Double.isFinite(qualityScore) || qualityScore < 0.0 || qualityScore > 1.0) {
            throw new IllegalArgumentException("qualityScore must be between 0 and 1");
        }
    }
}

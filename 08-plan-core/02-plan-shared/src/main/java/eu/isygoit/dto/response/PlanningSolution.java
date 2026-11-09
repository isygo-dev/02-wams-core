package eu.isygoit.dto.response;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * The plan: who does what, when, plus the quality report.
 * <p>
 * Ordering is deterministic: assignments by (startTick, taskId), unassigned tasks by task id, violations by
 * (constraintId, taskId, resourceId), quality details in objective request order.
 *
 * @param assignments     sorted by startTick, then task id
 * @param unassignedTasks ONLY unselected optional tasks (a required task is never unassigned)
 * @param hardViolations  always empty for a valid solution; relaxed hard constraints land here
 * @param softViolations  instance-level, sorted by constraint id, task id
 * @param qualityDetails  one per objective, evaluated independently by the validator
 * @param qualityScore    1.0 if OPTIMAL else {@code 1/(1+gap)} clamped to [0.01, 0.99]; 0.5 when the gap is unknown
 */
public record PlanningSolution(
        List<Assignment> assignments,
        List<UnassignedTask> unassignedTasks,
        List<ConstraintViolation> hardViolations,
        List<ConstraintViolation> softViolations,
        List<ObjectiveScore> qualityDetails,
        double qualityScore) {

    public PlanningSolution {
        assignments = CollectionCopies.list(assignments);
        unassignedTasks = CollectionCopies.list(unassignedTasks);
        hardViolations = CollectionCopies.list(hardViolations);
        softViolations = CollectionCopies.list(softViolations);
        qualityDetails = CollectionCopies.list(qualityDetails);
        if (!Double.isFinite(qualityScore) || qualityScore < 0.0 || qualityScore > 1.0) {
            throw new IllegalArgumentException("qualityScore must be between 0 and 1");
        }
    }
}

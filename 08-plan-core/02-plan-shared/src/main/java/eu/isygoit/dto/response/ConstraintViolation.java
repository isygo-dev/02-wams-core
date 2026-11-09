package eu.isygoit.dto.response;

/**
 * One violated instance of a constraint (instance level: one task, one resource, one pair...).
 *
 * @param constraintId the violated constraint
 * @param taskId       the task concerned, may be null
 * @param resourceId   the resource concerned, may be null
 * @param code         family specific violation code
 * @param message      human readable detail
 * @param impact       {@code weight * measure} (scaled value divided by the scale)
 */
public record ConstraintViolation(
        String constraintId,
        String taskId,
        String resourceId,
        String code,
        String message,
        double impact) {
}

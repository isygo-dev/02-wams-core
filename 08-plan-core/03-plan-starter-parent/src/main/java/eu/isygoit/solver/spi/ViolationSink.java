package eu.isygoit.solver.spi;

/**
 * Receiver of the violated instances found by a {@link ConstraintChecker}.
 * <p>
 * Implemented by the validator plumbing (WP6); called by checkers, on the validating thread only (not thread-safe, one
 * sink per check run). Units: {@code measure} is expressed in the family's own unit (ticks or counts, never Instants).
 */
public interface ViolationSink {

    /**
     * Reports one violated instance.
     *
     * @param taskId     id of the offending task, or {@code null} when not applicable
     * @param resourceId id of the offending resource, or {@code null} when not applicable
     * @param code       short UPPER_SNAKE violation code (family specific)
     * @param message    human readable English message
     * @param measure    violated family units of this instance (ticks, tasks, ...); must be {@code >= 1}
     */
    void violation(String taskId, String resourceId, String code, String message, long measure);
}

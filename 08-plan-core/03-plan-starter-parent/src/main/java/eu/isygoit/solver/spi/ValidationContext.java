package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;

import java.time.Instant;

/**
 * Collector handed to {@link ConstraintFamily#validate} / {@link ObjectiveFamily#validate}: read access to the problem
 * and the id sets, plus issue reporting. Validation collects ALL issues instead of failing on the first.
 * <p>
 * Implemented by the validation stage (WP2: {@code PlanningRequestValidator} / {@code ProblemVerifier}); used on a single
 * thread, one instance per validation run (not thread-safe). Issue {@code path} values are JSON paths such as
 * {@code constraints[2].params.minRestTicks}.
 */
public interface ValidationContext {

    /** @return the (normalized, or request-derived) problem being validated */
    PlanningProblem problem();

    /**
     * @return the read-only index; may be a LIGHT view when validation runs before the index exists (only counts, ids,
     * id-to-index lookups, horizon, limits and scope resolution are then guaranteed, see {@link CandidateIndexView})
     */
    CandidateIndexView index();

    /**
     * Strictly binds {@code c.params()} to {@code type} (unknown or missing properties fail).
     *
     * @return the bound params, or {@code null} after reporting {@code PARAMS_INVALID} when binding fails
     */
    <P> P params(Constraint c, Class<P> type);

    /** Reports an error issue (the request is rejected once validation ends). */
    void error(String code, String path, String message);

    /** Reports a warning issue (the request is accepted). */
    void warning(String code, String path, String message);

    /** @return {@code true} when a task with this id exists */
    boolean hasTask(String id);

    /** @return {@code true} when a resource with this id exists */
    boolean hasResource(String id);

    /** @return {@code true} when a slot with this id exists */
    boolean hasSlot(String id);

    /**
     * Converts an instant to a tick of the horizon (exact, aligned).
     *
     * @param t    the instant
     * @param path the JSON path reported when the conversion fails
     * @return the tick, or {@code -1} after reporting {@code TIME_NOT_ALIGNED} / {@code TIME_OUT_OF_HORIZON}
     */
    int tick(Instant t, String path);
}

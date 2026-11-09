package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Constraint;

/**
 * Independent, solver-free checker of one constraint family on a finished solution (design 8.2).
 * <p>
 * Implemented by the family (returned from {@link ConstraintFamily#checker()}); called by
 * {@code PlanningSolutionValidator}. Implementations must be stateless and thread-safe (they are Spring singletons) and
 * must not use any solver. All times are integer ticks.
 */
public interface ConstraintChecker {

    /**
     * Reports every violated INSTANCE of {@code constraint} in {@code ctx.solution()}. {@code measure} is expressed in
     * family units (ticks, count); the instances reported here must match the unit the contributors penalise, so that
     * model value and checker value are equal.
     *
     * @param ctx        the solution and problem view
     * @param constraint the constraint to check (its scope is resolved through {@code ctx.index()})
     * @param params     the already bound typed params record (type = {@link ConstraintFamily#paramsType()})
     * @param sink       receives every violation
     */
    void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink);
}

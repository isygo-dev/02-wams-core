package eu.isygoit.solver;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.solver.spi.ConstraintFamily;

/**
 * A constraint resolved against its family: the typed params are bound once. Created by {@code PlanningService.compile}
 * (WP2), consumed by the index builder, engines, contributors and the validator. Immutable.
 *
 * @param index      position in {@link CompiledProblem#constraints()} (stable, request order)
 * @param constraint the normalized constraint
 * @param family     the engine-neutral family
 * @param params     the bound typed params record (type = {@code family.paramsType()})
 * @param active     {@code false} when fully handled by preprocess / task rewrite (engines must skip it)
 */
public record CompiledConstraint(int index, Constraint constraint, ConstraintFamily family, Object params, boolean active) {
}

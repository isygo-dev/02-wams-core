package eu.isygoit.solver;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.solver.spi.ObjectiveFamily;

/**
 * An objective resolved against its family: typed params bound, tier and scale materialised. Created by
 * {@code PlanningService.compile} (WP2); consumed by engines, contributors and the validator. Immutable.
 *
 * @param index     position in {@link CompiledProblem#objectives()} (stable)
 * @param objective the normalized objective
 * @param family    the engine-neutral family
 * @param params    the bound typed params record, or {@code null} when the family has none ({@code Void.class})
 * @param tier      the effective lexicographic tier (-2 relaxation, -1 auto selection, 0..7 user tiers)
 * @param scale     the effective decimal-to-long multiplier for this objective
 */
public record CompiledObjective(int index, Objective objective, ObjectiveFamily family, Object params, int tier, int scale) {
}

package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Objective;

/**
 * Independent exact evaluator of one objective term on a finished solution (design 8.2).
 * <p>
 * Implemented by the {@link ObjectiveFamily}; called by the validator. Stateless, thread-safe, no solver. Must use the
 * same {@code ScaledMath} arithmetic as the engines so that the value equals the engine's tier contribution.
 */
public interface ObjectiveEvaluator {

    /**
     * Exact value of the term BEFORE the objective weight is applied (tick/count measures are exact integers; decimal
     * inputs are multiplied by the objective scale via {@link CheckContext#scaled(double, int)}). Maximisation terms
     * return the positive reward (sign-normalised).
     *
     * @param ctx       the solution and problem view
     * @param objective the objective (scale already materialised by the normalizer)
     * @param params    the already bound typed params record, or {@code null} when the family has none
     * @return the exact term value
     */
    long evaluate(CheckContext ctx, Objective objective, Object params);
}

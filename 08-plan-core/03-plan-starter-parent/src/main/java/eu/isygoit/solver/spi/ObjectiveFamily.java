package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Objective;

import java.util.List;

/**
 * Engine-neutral definition of one objective family of the catalogue (design 4.4 / 5.2).
 * <p>
 * Implemented by one Spring bean per objective (package {@code eu.isygoit.solver.objective}); discovered by
 * {@link eu.isygoit.solver.registry.ObjectiveFamilyRegistry}. Stateless and thread-safe. Per-engine formulations live in
 * contributors registered under the same {@link #type()}.
 */
public interface ObjectiveFamily {

    /** @return the family id, lower-case snake_case, for example {@code "minimize_makespan"} */
    String type();

    /** @return the typed params record, or {@code Void.class} when the family has no params */
    Class<?> paramsType();

    /** @return the i18n key, for example {@code "plan.objective.minimize_makespan.description"} */
    String descriptionKey();

    /** @return the params property names for the UI (empty when none) */
    List<String> paramsHint();

    /** @return {@code true} for a maximisation term (internally negated, reported sign-normalised) */
    boolean reward();

    /** Structural validation; report through {@code ctx}, never throw for user errors. {@code params} is already bound. */
    void validate(ValidationContext ctx, Objective objective, Object params);

    /** Estimates the model cost BEFORE building. @return never {@code null} */
    ModelCost estimate(CandidateIndexView index, Objective objective, Object params);

    /**
     * Exact value of the term (before weight) for a finished solution, same {@code ScaledMath} arithmetic as the engines.
     *
     * @return the stateless evaluator; never {@code null}
     */
    ObjectiveEvaluator evaluator();
}

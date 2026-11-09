package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.enums.IEnumEnforcement;

import java.util.List;
import java.util.Set;

/**
 * Engine-neutral definition of one constraint family of the catalogue (design 4.3 / 5.2): identity, typed params,
 * validation, cost estimation, optional neutral preprocessing and an independent checker.
 * <p>
 * Implemented by one Spring bean per family (package {@code eu.isygoit.solver.family}); discovered by
 * {@link eu.isygoit.solver.registry.ConstraintFamilyRegistry}; called by the validator, {@code ProblemVerifier},
 * {@code CandidateIndexBuilder} and {@code PlanningSolutionValidator}. Implementations are stateless and thread-safe.
 * Per-engine formulations live in separate contributors ({@code CpSatConstraintContributor} /
 * {@code TimefoldConstraintContributor}) registered under the same {@link #type()}. Time values are integer ticks.
 */
public interface ConstraintFamily {

    /** @return the family id, lower-case snake_case, for example {@code "min_rest"} */
    String type();

    /** @return the typed params record bound with strict Jackson, for example {@code MinRestParams.class} */
    Class<?> paramsType();

    /** @return the enforcement modes this family supports semantically (engine independent) */
    Set<IEnumEnforcement.Types> supportedEnforcements();

    /** @return the i18n key, for example {@code "plan.constraint.min_rest.description"} */
    String descriptionKey();

    /** @return the params property names, for the UI, for example {@code ["minRestTicks","afterTasks"]} */
    List<String> paramsHint();

    /** @return what {@code Constraint.scope.kind} is validated against */
    ScopeKind scopeKind();

    /**
     * Structural validation. Report through {@code ctx}; never throw for user errors.
     *
     * @param ctx        validation context
     * @param constraint the constraint
     * @param params     the already bound params
     */
    void validate(ValidationContext ctx, Constraint constraint, Object params);

    /**
     * Estimates the model cost BEFORE building (candidates and index are available).
     *
     * @return the cost; never {@code null} ({@link ModelCost#ZERO} when negligible)
     */
    ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params);

    /** Neutral compile-time step; called only when the constraint is HARD. Default: nothing. */
    default void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
    }

    /**
     * Independent checker used by {@code PlanningSolutionValidator} (design 8.2). Must not use any solver.
     *
     * @return the stateless checker; never {@code null}
     */
    ConstraintChecker checker();
}

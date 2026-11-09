package eu.isygoit.solver.cpsat;

import eu.isygoit.solver.CompiledConstraint;

/**
 * CP-SAT formulation of one constraint family (design 5.5).
 * <p>
 * Implemented by one Spring bean per family ({@code solver.cpsat.contrib}); discovered by
 * {@link eu.isygoit.solver.registry.CpSatContributorRegistry}; called by {@code CpModelBuilder} once per active
 * constraint per build, on the building thread. Stateless and thread-safe (state lives in {@link CpModelContext}).
 * <p>
 * Universal contributor rule: every model constraint that expresses the RULE must be enforced by
 * {@code enforcer.instance(..)} (null = unconditional); definitional channeling constraints must NOT be enforced.
 */
public interface CpSatConstraintContributor {

    /** @return the same id as the {@code ConstraintFamily} it formulates */
    String type();

    /** @return which of HARD / SOFT / RELAXABLE this FORMULATION implements */
    EnforcementSupport support();

    /**
     * Adds the constraint to {@code ctx.model()}. Allocations must call {@code ctx.count(..)}; loops must call
     * {@code ctx.checkpoint()}.
     *
     * @param ctx        the per-build model context
     * @param constraint the compiled constraint (typed params in {@code constraint.params()})
     * @param enforcer   hides HARD / RELAXABLE / SOFT / EXPLAIN (D-7); one per constraint per build
     */
    void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer);
}

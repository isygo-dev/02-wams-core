package eu.isygoit.solver.timefold;

import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.EnforcementSupport;

/**
 * Timefold formulation of one constraint family (design 5.6). Contributes to option-level data only: there is NO Timefold
 * stream code in contributors.
 * <p>
 * Implemented by one Spring bean per supported family ({@code solver.timefold.contrib}); discovered by
 * {@link eu.isygoit.solver.registry.TimefoldContributorRegistry}; called once per active constraint while the
 * {@code TimefoldBuildContext} is filled, on the building thread. Stateless and thread-safe.
 */
public interface TimefoldConstraintContributor {

    /** @return the same id as the {@code ConstraintFamily} it formulates */
    String type();

    /** @return which of HARD / SOFT / RELAXABLE this formulation implements (Timefold: hard and soft only) */
    EnforcementSupport support();

    /**
     * Writes the constraint into the option-level data of {@code ctx} (forbid options, add option penalties, ...).
     *
     * @param ctx        the build context
     * @param constraint the compiled constraint
     */
    void contribute(TimefoldBuildContext ctx, CompiledConstraint constraint);
}

package eu.isygoit.solver.cpsat;

import eu.isygoit.solver.CompiledObjective;

/**
 * CP-SAT formulation of one objective family (design 5.5).
 * <p>
 * Implemented by one Spring bean per objective ({@code solver.cpsat.objective}); discovered by
 * {@link eu.isygoit.solver.registry.CpSatContributorRegistry}; called by {@code CpModelBuilder} once per objective per
 * build, on the building thread. Stateless and thread-safe (state lives in {@link CpModelContext}).
 */
public interface CpSatObjectiveContributor {

    /** @return the same id as the {@code ObjectiveFamily} it formulates */
    String type();

    /**
     * Contributes terms with {@code sink.add(tier, expr, min, max, label)}. Maximisation: pass the negated expression.
     * Allocations must call {@code ctx.count(..)}; loops must call {@code ctx.checkpoint()}.
     */
    void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink);
}

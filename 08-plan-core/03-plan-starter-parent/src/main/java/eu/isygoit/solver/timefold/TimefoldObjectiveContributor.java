package eu.isygoit.solver.timefold;

import eu.isygoit.solver.CompiledObjective;

/**
 * Timefold formulation of one objective family (design 5.6).
 * <p>
 * Implemented by one Spring bean per supported objective ({@code solver.timefold.contrib}); discovered by
 * {@link eu.isygoit.solver.registry.TimefoldContributorRegistry}; called once per objective while the
 * {@code TimefoldBuildContext} is filled, on the building thread. Stateless and thread-safe.
 */
public interface TimefoldObjectiveContributor {

    /** @return the same id as the {@code ObjectiveFamily} it formulates */
    String type();

    /** Adds the exact scaled cost of every (task, resource, slot) OPTION and/or the selection reward of a task. */
    void contribute(TimefoldBuildContext ctx, CompiledObjective objective);
}

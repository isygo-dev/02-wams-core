package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Task;

/**
 * Optional second interface of a {@link ConstraintFamily} that REWRITES tasks before indexing
 * ({@code multi_resource_requirement}).
 * <p>
 * Implemented by the family; called by {@code DomainNormalizer} once per (scoped task, HARD constraint); the constraint is
 * then marked {@code active=false} in the {@link eu.isygoit.solver.CompiledConstraint}. Must be pure (no side effects),
 * deterministic and thread-safe; tasks are immutable records, so return a new instance.
 */
public interface TaskRewriter {

    /**
     * @param task       the task to rewrite (a member of the constraint scope)
     * @param constraint the HARD constraint being applied
     * @param params     the bound typed params record
     * @return the rewritten task (may be the same instance when nothing changes)
     */
    Task rewrite(Task task, Constraint constraint, Object params);
}

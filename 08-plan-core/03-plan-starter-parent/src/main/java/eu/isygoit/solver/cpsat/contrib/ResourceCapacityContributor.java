package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.params.ResourceCapacityParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.ResourceCapacityFamily;
import eu.isygoit.solver.spi.CandidateIndexView;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code resource_capacity} (design 6.5.2): for every scoped resource whose capacity is lowered, a
 * fixed block of {@code capacity - newCapacity} units over the window is registered with
 * {@link CpModelContext#blockResource}; the NoOverlap / Cumulative of the resource is created from the blocks at model
 * finalisation (the global constraints cannot carry an enforcement literal, so the block itself is optional when the
 * enforcer returns a literal).
 * <p>
 * Supports HARD and RELAXABLE only (the catalogue defines no soft unit); a SOFT enforcer is a programming error and is
 * rejected. HARD solve: the block is unconditional; RELAXABLE: guarded by the shared {@code active_k} literal; explain
 * pass: guarded by the assumption literal. Stateless and thread-safe; units are ticks.
 */
@Component
public class ResourceCapacityContributor implements CpSatConstraintContributor {

    private static final EnforcementSupport SUPPORT = new EnforcementSupport(true, false, true);

    @Override
    public String type() {
        return ResourceCapacityFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return SUPPORT;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        if (enforcer.mode() == Enforcer.Mode.SOFT) {
            throw new IllegalStateException("resource_capacity does not support SOFT enforcement (constraint "
                    + constraint.constraint().id() + ")");
        }
        ResourceCapacityParams p = (ResourceCapacityParams) constraint.params();
        CandidateIndexView idx = ctx.index();
        int[] window = ResourceCapacityFamily.windowTicks(idx.horizon(), p);
        if (window[0] >= window[1]) {
            return; // the window lies outside the horizon: nothing to block
        }
        int blocks = 0;
        for (int r : idx.resolveResources(constraint.constraint().scope())) {
            ctx.checkpoint();
            int capacity = idx.capacity(r);
            int newCapacity = ResourceCapacityFamily.newCapacity(capacity, p);
            if (newCapacity >= capacity) {
                continue;
            }
            // unary resource: newCapacity can only be 0 => demand 1 = the whole capacity
            ctx.blockResource(r, window[0], window[1], capacity - newCapacity, enforcer.instance(-1, r));
            blocks++;
        }
        ctx.count(0, 0, 0, blocks);
    }
}

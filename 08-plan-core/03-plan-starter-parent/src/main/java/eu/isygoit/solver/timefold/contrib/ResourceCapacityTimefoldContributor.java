package eu.isygoit.solver.timefold.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceCapacityParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.timefold.SlotOptions;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import org.springframework.stereotype.Component;

/**
 * Timefold formulation of {@code resource_capacity} (design 4.3): lowers the capacity of the scoped resources to
 * {@code min(Resource.capacity, params.capacity)}, optionally only inside {@code params.window}.
 * <p>
 * Timefold capacity is per (resource, slot) bucket, so a window affects a bucket when the window OVERLAPS the slot (a
 * window that only covers part of a slot lowers the whole slot: the conservative, never-violating choice; slot-aligned
 * windows, the documented subset, are exact). The window is widened to the tick grid (start floored, end ceiled), like
 * every blocking interval. Options whose demand no longer fits are forbidden. HARD only (no soft, no relaxation).
 * Stateless Spring bean, thread-safe; runs on the building thread. Scope: RESOURCES.
 */
@Component
public class ResourceCapacityTimefoldContributor implements TimefoldConstraintContributor {

    /** The family id. */
    public static final String TYPE = "resource_capacity";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void contribute(TimefoldBuildContext ctx, CompiledConstraint compiled) {
        if (!compiled.active() || !(compiled.params() instanceof ResourceCapacityParams params)
                || params.capacity() == null) {
            return;
        }
        Constraint constraint = compiled.constraint();
        CandidateIndex index = ctx.index();
        boolean[] resourceMask = ctx.resourceMask(constraint.scope());
        TimeWindow window = params.window();
        long windowStart = Long.MIN_VALUE;
        long windowEnd = Long.MAX_VALUE;
        if (window != null) {
            Horizon horizon = index.horizon();
            if (window.start() != null) {
                windowStart = horizon.floorTick(window.start());
            }
            if (window.end() != null) {
                windowEnd = horizon.ceilTick(window.end());
            }
        }
        SlotOptions options = ctx.options();
        for (int o = 0; o < options.optionCount(); o++) {
            if (resourceMask != null && !resourceMask[options.resource(o)]) {
                continue;
            }
            int slot = options.slot(o);
            boolean overlaps = window == null
                    || (index.slotStart[slot] < windowEnd && windowStart < index.slotEnd[slot]);
            if (!overlaps) {
                continue;
            }
            ctx.lowerCapacity(o, params.capacity());
            if (options.demand(o) > ctx.optionCapacity()[o]) {
                ctx.forbid(o, constraint.id());
            }
        }
    }
}

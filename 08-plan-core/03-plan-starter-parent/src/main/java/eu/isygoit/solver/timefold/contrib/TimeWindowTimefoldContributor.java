package eu.isygoit.solver.timefold.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.params.TimeWindowParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.timefold.SlotOptions;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import org.springframework.stereotype.Component;

/**
 * Timefold formulation of {@code time_window} (design 4.3): extra windows on the scoped tasks, as NEUTRAL pruning of the
 * (task, resource, slot) options: a slot start is allowed when {@code start >= earliestStart}, {@code start <=
 * latestStart}, {@code end >= earliestEnd} and {@code end <= latestEnd} (null bound = no bound). Bounds are rounded
 * towards the inside of the window on the tick grid ({@code ceilTick} for lower, {@code floorTick} for upper bounds), so
 * the window only ever shrinks. Idempotent with the family preprocess (a constraint with {@code active == false} is
 * skipped). HARD only. Stateless Spring bean, thread-safe; runs on the building thread. Scope: TASKS.
 */
@Component
public class TimeWindowTimefoldContributor implements TimefoldConstraintContributor {

    /** The family id. */
    public static final String TYPE = "time_window";

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
        if (!compiled.active() || !(compiled.params() instanceof TimeWindowParams params)) {
            return;
        }
        Constraint constraint = compiled.constraint();
        Horizon horizon = ctx.index().horizon();
        long earliestStart = params.earliestStart() == null ? Long.MIN_VALUE : horizon.ceilTick(params.earliestStart());
        long latestStart = params.latestStart() == null ? Long.MAX_VALUE : horizon.floorTick(params.latestStart());
        long earliestEnd = params.earliestEnd() == null ? Long.MIN_VALUE : horizon.ceilTick(params.earliestEnd());
        long latestEnd = params.latestEnd() == null ? Long.MAX_VALUE : horizon.floorTick(params.latestEnd());
        boolean[] taskMask = ctx.taskMask(constraint.scope());
        SlotOptions options = ctx.options();
        for (int t = 0; t < options.taskCount(); t++) {
            if (taskMask != null && !taskMask[t]) {
                continue;
            }
            for (int o = options.taskOptionStart(t); o < options.taskOptionEnd(t); o++) {
                int start = options.start(o);
                int end = options.end(o);
                if (start < earliestStart || start > latestStart || end < earliestEnd || end > latestEnd) {
                    ctx.forbid(o, constraint.id());
                }
            }
        }
    }
}

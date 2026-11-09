package eu.isygoit.solver.timefold.contrib;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.timefold.SlotOptions;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldObjectiveContributor;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

/**
 * Timefold formulation of {@code minimize_resource_cost} (design 4.4), {@code costPerTick} part only: every option costs
 * {@code weight * scale(costPerTick_r, objective.scale) * duration} ticks. {@code costPerUse} needs a "resource used"
 * indicator, which the Timefold subset does not have: it is rejected at validation ({@code COST_PER_USE}) and
 * re-checked by the engine, never silently ignored here. The objective scope may select tasks or resources.
 * Weight 0 = disabled. Stateless Spring bean, thread-safe; building thread.
 */
@Component
public class MinimizeResourceCostTimefoldContributor implements TimefoldObjectiveContributor {

    /** The family id. */
    public static final String TYPE = "minimize_resource_cost";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void contribute(TimefoldBuildContext ctx, CompiledObjective compiled) {
        Objective objective = compiled.objective();
        if (objective.weight() == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        SlotOptions options = ctx.options();
        boolean resourceScope = TimefoldBuildContext.isResourceScope(objective.scope());
        boolean[] taskMask = resourceScope ? null : ctx.taskMask(objective.scope());
        boolean[] resourceMask = resourceScope ? ctx.resourceMask(objective.scope()) : null;
        long[] weightedTickCost = new long[index.resourceCount()];
        boolean[] computed = new boolean[index.resourceCount()];
        for (int t = 0; t < options.taskCount(); t++) {
            if (taskMask != null && !taskMask[t]) {
                continue;
            }
            for (int o = options.taskOptionStart(t); o < options.taskOptionEnd(t); o++) {
                int resource = options.resource(o);
                if (resourceMask != null && !resourceMask[resource]) {
                    continue;
                }
                if (!computed[resource]) {
                    Double costPerTick = ctx.resource(resource).costPerTick();
                    weightedTickCost[resource] = costPerTick == null ? 0L : ScaledMath.mulExact(objective.weight(),
                            ScaledMath.scale(costPerTick, compiled.scale()));
                    computed[resource] = true;
                }
                if (weightedTickCost[resource] != 0L) {
                    long duration = options.end(o) - options.start(o);
                    ctx.addPenalty(o, ScaledMath.mulExact(weightedTickCost[resource], duration));
                }
            }
        }
    }
}

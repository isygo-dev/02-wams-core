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
 * Timefold formulation of {@code minimize_slot_cost} (design 4.4): every option costs
 * {@code weight * scale(slot.cost, objective.scale)} for the slot it starts in (slot-mode tasks of the objective scope).
 * Same {@link ScaledMath} arithmetic as the CP-SAT contributor and the evaluator, so the tier value is identical
 * (G1: cost 1.0 at scale 100 = 100). Weight 0 = disabled. Stateless Spring bean, thread-safe; building thread.
 */
@Component
public class MinimizeSlotCostTimefoldContributor implements TimefoldObjectiveContributor {

    /** The family id. */
    public static final String TYPE = "minimize_slot_cost";

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
        long[] weightedCostBySlot = new long[index.slotCount()];
        boolean[] computed = new boolean[index.slotCount()];
        boolean[] taskMask = ctx.taskMask(objective.scope());
        for (int t = 0; t < options.taskCount(); t++) {
            if (taskMask != null && !taskMask[t]) {
                continue;
            }
            for (int o = options.taskOptionStart(t); o < options.taskOptionEnd(t); o++) {
                int slot = options.slot(o);
                if (!computed[slot]) {
                    weightedCostBySlot[slot] = ScaledMath.mulExact(objective.weight(),
                            ScaledMath.scale(index.slotCost[slot], compiled.scale()));
                    computed[slot] = true;
                }
                if (weightedCostBySlot[slot] != 0L) {
                    ctx.addPenalty(o, weightedCostBySlot[slot]);
                }
            }
        }
    }
}

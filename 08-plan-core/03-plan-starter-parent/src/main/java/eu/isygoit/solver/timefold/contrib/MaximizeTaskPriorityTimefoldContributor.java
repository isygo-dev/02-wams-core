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
 * Timefold formulation of {@code maximize_task_priority} (design 4.4): selecting an OPTIONAL task of the objective scope
 * rewards {@code weight * priority}; required tasks are always selected and do not contribute (v1 semantic). Integral
 * measure: not scaled. Weight 0 = disabled. Stateless Spring bean, thread-safe; building thread.
 */
@Component
public class MaximizeTaskPriorityTimefoldContributor implements TimefoldObjectiveContributor {

    /** The family id. */
    public static final String TYPE = "maximize_task_priority";

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
        boolean[] taskMask = ctx.taskMask(objective.scope());
        for (int t = 0; t < options.taskCount(); t++) {
            if (index.required(t) || (taskMask != null && !taskMask[t])) {
                continue;
            }
            ctx.addSelectionReward(t, ScaledMath.mulExact(objective.weight(), index.priority(t)));
        }
    }
}

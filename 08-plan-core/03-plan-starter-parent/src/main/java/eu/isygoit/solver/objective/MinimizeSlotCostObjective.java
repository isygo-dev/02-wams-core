package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code minimize_slot_cost} (design 4.4): {@code sum scale(slot.cost)} of the slot chosen by the start of each selected
 * slot-mode task of the scope. The slot of a task is {@code SolutionView.slotOf(task)}; when several allowed slots of a
 * task share the same start tick the LOWEST slot index wins (the CP-SAT contributor uses the same rule).
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeSlotCostContributor}.
 */
@Component
public class MinimizeSlotCostObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_slot_cost";

    public MinimizeSlotCostObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.TASKS);
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        PlanningProblem problem = ctx.problem();
        int scale = ObjectiveMath.scaleOf(problem, objective);
        boolean anyCost = false;
        for (int i = 0; i < problem.slots().size(); i++) {
            Slot slot = problem.slots().get(i);
            try {
                anyCost |= ScaledMath.scale(slot.cost(), scale) > 0;
            } catch (ObjectiveRangeException e) {
                ctx.error(ObjectiveRangeException.CODE, "slots[" + i + "].cost", e.getMessage());
            }
        }
        if (!anyCost) {
            noEffect(ctx, objective, "no slot has a positive cost at scale " + scale);
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        long slots = index.slotCount();
        long tasks = index.taskCount();
        return new ModelCost(tasks * slots, 0, tasks, 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        PlanningProblem problem = ctx.problem();
        SolutionView solution = ctx.solution();
        int scale = ObjectiveMath.scaleOf(problem, objective);
        long total = 0;
        for (int t : ctx.index().resolveTasks(objective.scope())) {
            if (!solution.selected(t)) {
                continue;
            }
            int slot = solution.slotOf(t);
            if (slot >= 0) {
                total = ScaledMath.addExact(total, ObjectiveMath.slotCost(problem, slot, scale));
            }
        }
        return ScaledMath.guard(total, "minimize_slot_cost");
    }
}

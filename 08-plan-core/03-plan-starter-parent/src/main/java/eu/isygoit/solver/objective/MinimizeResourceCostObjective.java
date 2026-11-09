package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
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
 * {@code minimize_resource_cost} (design 4.4): {@code sum over assigned (task, resource) of scale(costPerTick_r) *
 * duration} plus {@code sum over used resources of scale(costPerUse_r)}, over the resources of the scope. Decimal inputs
 * are scaled by the objective scale with {@link ScaledMath#scale(double, int)}; durations are exact ticks.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeResourceCostContributor}.
 */
@Component
public class MinimizeResourceCostObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_resource_cost";

    public MinimizeResourceCostObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.RESOURCES);
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        PlanningProblem problem = ctx.problem();
        int scale = ObjectiveMath.scaleOf(problem, objective);
        boolean anyCost = false;
        for (int i = 0; i < problem.resources().size(); i++) {
            Resource r = problem.resources().get(i);
            try {
                anyCost |= ScaledMath.scale(r.costPerTick(), scale) > 0;
            } catch (ObjectiveRangeException e) {
                ctx.error(ObjectiveRangeException.CODE, "resources[" + i + "].costPerTick", e.getMessage());
            }
            try {
                anyCost |= ScaledMath.scale(r.costPerUse(), scale) > 0;
            } catch (ObjectiveRangeException e) {
                ctx.error(ObjectiveRangeException.CODE, "resources[" + i + "].costPerUse", e.getMessage());
            }
        }
        if (!anyCost) {
            noEffect(ctx, objective, "every resource has costPerTick and costPerUse 0 at scale " + scale);
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(index.resourceCount(), 0, index.resourceCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        PlanningProblem problem = ctx.problem();
        SolutionView solution = ctx.solution();
        int scale = ObjectiveMath.scaleOf(problem, objective);
        long total = 0;
        for (int r : ctx.index().resolveResources(objective.scope())) {
            int[] tasks = solution.tasksOfResource(r);
            if (tasks.length == 0) {
                continue;
            }
            Resource resource = problem.resources().get(r);
            long perTick = ctx.scaled(resource.costPerTick(), scale);
            total = ScaledMath.addExact(total, ctx.scaled(resource.costPerUse(), scale));
            for (int t : tasks) {
                long ticks = (long) solution.end(t) - solution.start(t);
                total = ScaledMath.addExact(total, ScaledMath.mulExact(perTick, ticks));
            }
        }
        return ScaledMath.guard(total, "minimize_resource_cost");
    }
}

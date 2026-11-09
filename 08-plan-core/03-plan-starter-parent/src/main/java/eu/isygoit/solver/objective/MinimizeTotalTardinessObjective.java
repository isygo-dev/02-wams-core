package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.params.TardinessParams;
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
 * {@code minimize_total_tardiness} (design 4.4): {@code sum max(0, end_t - dueDate_t)} over the selected tasks of the
 * scope that have a due date; with {@code weightBy = PRIORITY} each tardiness is multiplied by {@code 1 + priority}.
 * Ticks, never scaled.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeTotalTardinessContributor}.
 */
@Component
public class MinimizeTotalTardinessObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_total_tardiness";

    public MinimizeTotalTardinessObjective() {
        super(TYPE, TardinessParams.class, false, List.of("weightBy"), ScopeKind.TASKS);
    }

    /** @return the weighting of the (possibly {@code null}) bound params, default NONE */
    public static TardinessParams.WeightBy weightBy(Object params) {
        return params instanceof TardinessParams p && p.weightBy() != null ? p.weightBy() : TardinessParams.WeightBy.NONE;
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        boolean anyDue = false;
        for (Task t : ctx.problem().tasks()) {
            anyDue |= t.dueDate() != null;
        }
        if (!anyDue) {
            noEffect(ctx, objective, "no task has a dueDate");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, index.taskCount(), index.taskCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        boolean byPriority = weightBy(params) == TardinessParams.WeightBy.PRIORITY;
        SolutionView solution = ctx.solution();
        long total = 0;
        for (int t : ctx.index().resolveTasks(objective.scope())) {
            int due = ctx.index().due(t);
            if (!solution.selected(t) || due == ObjectiveMath.NO_DUE) {
                continue;
            }
            long tardiness = ObjectiveMath.tardiness(solution.end(t), due);
            long factor = byPriority ? 1L + ctx.index().priority(t) : 1L;
            total = ScaledMath.addExact(total, ScaledMath.mulExact(tardiness, factor));
        }
        return ScaledMath.guard(total, "minimize_total_tardiness");
    }
}

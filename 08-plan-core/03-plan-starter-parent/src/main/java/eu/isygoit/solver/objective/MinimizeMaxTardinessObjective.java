package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code minimize_max_tardiness} (design 4.4): {@code max_t max(0, end_t - dueDate_t)} over the selected tasks of the
 * scope that have a due date, in ticks (0 when none).
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeMaxTardinessContributor}.
 */
@Component
public class MinimizeMaxTardinessObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_max_tardiness";

    public MinimizeMaxTardinessObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.TASKS);
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
        return new ModelCost(0, 1, index.taskCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        SolutionView solution = ctx.solution();
        long max = 0;
        for (int t : ctx.index().resolveTasks(objective.scope())) {
            int due = ctx.index().due(t);
            if (solution.selected(t) && due != ObjectiveMath.NO_DUE) {
                max = Math.max(max, ObjectiveMath.tardiness(solution.end(t), due));
            }
        }
        return max;
    }
}

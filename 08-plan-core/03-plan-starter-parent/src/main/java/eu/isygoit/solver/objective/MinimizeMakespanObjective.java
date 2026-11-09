package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code minimize_makespan} (design 4.4): the term is {@code M = max end} over the SELECTED tasks of the scope, in ticks
 * (0 when none is selected). Integral measure, never scaled.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeMakespanContributor}.
 */
@Component
public class MinimizeMakespanObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_makespan";

    public MinimizeMakespanObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.TASKS);
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
            if (solution.selected(t)) {
                max = Math.max(max, solution.end(t));
            }
        }
        return max;
    }
}

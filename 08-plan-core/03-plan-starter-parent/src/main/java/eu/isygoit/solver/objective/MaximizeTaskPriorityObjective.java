package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Task;
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
 * {@code maximize_task_priority} (design 4.4, v1 semantic): reward {@code sum priority_t} over the SELECTED OPTIONAL
 * tasks of the scope. Maximisation: the evaluator returns the positive reward; the engine minimises its negation.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MaximizeTaskPriorityContributor}.
 */
@Component
public class MaximizeTaskPriorityObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "maximize_task_priority";

    public MaximizeTaskPriorityObjective() {
        super(TYPE, Void.class, true, List.of(), ScopeKind.TASKS);
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        boolean anyOptional = false;
        for (Task t : ctx.problem().tasks()) {
            anyOptional |= !t.requiredTask();
        }
        if (!anyOptional) {
            noEffect(ctx, objective, "there is no optional task");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, 0, 0, 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        SolutionView solution = ctx.solution();
        long total = 0;
        for (int t : ctx.index().resolveTasks(objective.scope())) {
            if (!ctx.index().required(t) && solution.selected(t)) {
                total = ScaledMath.addExact(total, ctx.index().priority(t));
            }
        }
        return ScaledMath.guard(total, "maximize_task_priority");
    }
}

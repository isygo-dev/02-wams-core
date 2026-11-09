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
 * {@code maximize_selected_value} (design 4.4): reward {@code sum effectivePrize_t} ({@code prize ?? 1}) over the SELECTED
 * OPTIONAL tasks of the scope. Injected at the reserved tier -1 by {@link AutoObjectives} when a problem has optional
 * tasks and no selection objective. Maximisation: positive reward in the evaluator, negated in the engine.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MaximizeSelectedValueContributor}.
 */
@Component
public class MaximizeSelectedValueObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "maximize_selected_value";

    public MaximizeSelectedValueObjective() {
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
                total = ScaledMath.addExact(total, ctx.index().prize(t));
            }
        }
        return ScaledMath.guard(total, "maximize_selected_value");
    }
}

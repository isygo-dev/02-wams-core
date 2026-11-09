package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.params.PlanChangeParams;
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
 * {@code minimize_plan_changes} (design 4.4): deviation of the SELECTED tasks of the scope from the NON-LOCKED
 * {@code initialSolution} assignments: {@code startDeviationWeight * |start - initialStart|} per task (ticks) plus
 * {@code resourceChangeWeight} per initial resource that is not among the resources the task now uses (an unknown initial
 * resource always counts). Locked assignments cannot deviate and are skipped. Unselected tasks contribute 0.
 * Integers, never scaled.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizePlanChangesContributor}.
 */
@Component
public class MinimizePlanChangesObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_plan_changes";

    public MinimizePlanChangesObjective() {
        super(TYPE, PlanChangeParams.class, false, List.of("startDeviationWeight", "resourceChangeWeight"),
                ScopeKind.TASKS);
    }

    /** @return the start deviation weight of the (possibly {@code null}) params, default 1 */
    public static int startWeight(Object params) {
        return params instanceof PlanChangeParams p && p.startDeviationWeight() != null ? p.startDeviationWeight() : 1;
    }

    /** @return the resource change weight of the (possibly {@code null}) params, default 1 */
    public static int resourceWeight(Object params) {
        return params instanceof PlanChangeParams p && p.resourceChangeWeight() != null ? p.resourceChangeWeight() : 1;
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        if (startWeight(params) < 0) {
            ctx.error(OBJECTIVE_PARAMS_INVALID, path(ctx, objective, "params.startDeviationWeight"), "must be >= 0");
        }
        if (resourceWeight(params) < 0) {
            ctx.error(OBJECTIVE_PARAMS_INVALID, path(ctx, objective, "params.resourceChangeWeight"), "must be >= 0");
        }
        boolean anyHint = false;
        for (InitialAssignment a : ctx.problem().initialSolution().assignments()) {
            anyHint |= !a.locked();
        }
        if (!anyHint) {
            noEffect(ctx, objective, "initialSolution has no non-locked assignment");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, index.taskCount(), 2L * index.taskCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        SolutionView solution = ctx.solution();
        CandidateIndexView index = ctx.index();
        long startWeight = startWeight(params);
        long resourceWeight = resourceWeight(params);
        boolean[] inScope = new boolean[index.taskCount()];
        for (int t : index.resolveTasks(objective.scope())) {
            inScope[t] = true;
        }
        long total = 0;
        for (InitialAssignment a : ctx.problem().initialSolution().assignments()) {
            int t = index.taskIdx(a.taskId());
            if (a.locked() || t < 0 || !inScope[t] || !solution.selected(t)) {
                continue;
            }
            if (a.start() != null) {
                long deviation = Math.abs((long) solution.start(t) - index.horizon().toTick(a.start()));
                total = ScaledMath.addExact(total, ScaledMath.mulExact(startWeight, deviation));
            }
            for (String resourceId : a.resourceIds()) {
                int wanted = index.resourceIdx(resourceId);
                boolean kept = false;
                for (int k = 0; k < solution.resourceCount(t); k++) {
                    kept |= solution.resourceAt(t, k) == wanted;
                }
                if (!kept) {
                    total = ScaledMath.addExact(total, resourceWeight);
                }
            }
        }
        return ScaledMath.guard(total, "minimize_plan_changes");
    }
}

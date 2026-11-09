package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Resource;
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
 * {@code minimize_setup} (design 4.4): the total setup ticks of the adjacent task pairs of the resources of the scope
 * (the arcs of the setup circuit, 6.6). The first task of a resource has no setup. The checker walks
 * {@code tasksOfResource(r)} (ascending start) and sums {@link ObjectiveMath.SetupLookup#ticks}. Ticks, never scaled.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeSetupContributor} (reads {@code ctx.setupArcs(r)}).
 */
@Component
public class MinimizeSetupObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_setup";

    public MinimizeSetupObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.RESOURCES);
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        boolean anyMatrix = false;
        for (Resource r : ctx.problem().resources()) {
            anyMatrix |= r.setupMatrixId() != null;
        }
        if (!anyMatrix) {
            noEffect(ctx, objective, "no resource has a setupMatrixId");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, 0, 0, 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        SolutionView solution = ctx.solution();
        ObjectiveMath.SetupLookup lookup = ObjectiveMath.SetupLookup.of(ctx.problem());
        long total = 0;
        for (int r : ctx.index().resolveResources(objective.scope())) {
            int[] tasks = solution.tasksOfResource(r);
            for (int k = 1; k < tasks.length; k++) {
                total = ScaledMath.addExact(total, lookup.ticks(r, tasks[k - 1], tasks[k]));
            }
        }
        return ScaledMath.guard(total, "minimize_setup");
    }
}

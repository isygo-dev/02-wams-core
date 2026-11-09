package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.LoadObjectiveParams;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code minimize_max_load} (design 4.4, fairness min-max): {@code L = max_r load_r} over the resources of the scope that
 * have candidates; load = summed task durations (TICKS) or number of tasks (COUNT) of the resource. 0 when no such
 * resource exists.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeMaxLoadContributor}.
 */
@Component
public class MinimizeMaxLoadObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_max_load";

    public MinimizeMaxLoadObjective() {
        super(TYPE, LoadObjectiveParams.class, false, List.of("metric"), ScopeKind.RESOURCES);
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, 1, index.resourceCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        LoadMetric metric = ObjectiveMath.metricOf(params);
        boolean[] has = ObjectiveMath.resourcesWithCandidates(ctx.index());
        long max = 0;
        for (int r : ctx.index().resolveResources(objective.scope())) {
            if (has[r]) {
                max = Math.max(max, ObjectiveMath.load(ctx.solution(), r, metric));
            }
        }
        return max;
    }
}

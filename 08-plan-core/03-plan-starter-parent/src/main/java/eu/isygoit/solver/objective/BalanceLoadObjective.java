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
 * {@code balance_load} (design 4.4, fairness spread): {@code Lmax - Lmin} over the resources of the scope that have
 * candidates; load = summed task durations (TICKS) or number of tasks (COUNT). 0 with fewer than two such resources.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code BalanceLoadContributor}.
 */
@Component
public class BalanceLoadObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "balance_load";

    public BalanceLoadObjective() {
        super(TYPE, LoadObjectiveParams.class, false, List.of("metric"), ScopeKind.RESOURCES);
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, 2, 2L * index.resourceCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        LoadMetric metric = ObjectiveMath.metricOf(params);
        boolean[] has = ObjectiveMath.resourcesWithCandidates(ctx.index());
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        int count = 0;
        for (int r : ctx.index().resolveResources(objective.scope())) {
            if (has[r]) {
                long load = ObjectiveMath.load(ctx.solution(), r, metric);
                max = Math.max(max, load);
                min = Math.min(min, load);
                count++;
            }
        }
        return count < 2 ? 0L : max - min;
    }
}

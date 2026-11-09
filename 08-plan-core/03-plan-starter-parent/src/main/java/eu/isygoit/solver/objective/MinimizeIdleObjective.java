package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.params.LoadMetric;
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
 * {@code minimize_idle} (design 4.4): {@code sum_r (span_r - load_r)} where {@code span_r = lastEnd_r - firstStart_r} over
 * the tasks assigned to {@code r} and {@code load_r} is the summed duration; unused resources contribute 0.
 * <p>
 * Only UNARY resources (capacity 1) are considered: on a cumulative resource tasks overlap and {@code span - load} would
 * be negative, which is not an idle time (a warning is reported when resources with capacity above 1 exist, and both the
 * evaluator and the CP-SAT contributor skip them). Ticks, never scaled.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizeIdleContributor}.
 */
@Component
public class MinimizeIdleObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_idle";
    /** Warning code: cumulative resources are ignored by the idle objective. */
    public static final String IDLE_CUMULATIVE_IGNORED = "OBJECTIVE_IDLE_CUMULATIVE_IGNORED";

    public MinimizeIdleObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.RESOURCES);
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        for (int i = 0; i < ctx.problem().resources().size(); i++) {
            Integer capacity = ctx.problem().resources().get(i).capacity();
            if (capacity != null && capacity > 1) {
                ctx.warning(IDLE_CUMULATIVE_IGNORED, "resources[" + i + "].capacity",
                        "minimize_idle ignores the cumulative resource '" + ctx.problem().resources().get(i).id() + "'");
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        long resources = index.resourceCount();
        return new ModelCost(0, 2 * resources, 3 * resources, 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        SolutionView solution = ctx.solution();
        CandidateIndexView index = ctx.index();
        long total = 0;
        for (int r : index.resolveResources(objective.scope())) {
            if (index.capacity(r) != 1) {
                continue;
            }
            int[] tasks = solution.tasksOfResource(r);
            if (tasks.length == 0) {
                continue;
            }
            long first = Long.MAX_VALUE;
            long last = Long.MIN_VALUE;
            for (int t : tasks) {
                first = Math.min(first, solution.start(t));
                last = Math.max(last, solution.end(t));
            }
            total = ScaledMath.addExact(total, (last - first) - ObjectiveMath.load(solution, r, LoadMetric.TICKS));
        }
        return ScaledMath.guard(total, "minimize_idle");
    }
}

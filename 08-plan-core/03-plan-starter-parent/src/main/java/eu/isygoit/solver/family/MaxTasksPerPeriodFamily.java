package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.MaxTasksPerPeriodParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PeriodBuckets;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Component
public class MaxTasksPerPeriodFamily implements ConstraintFamily {

    public static final String TYPE = "max_tasks_per_period";
    public static final String CODE = "MAX_TASKS_PER_PERIOD_EXCEEDED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MaxTasksPerPeriodParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.HARD_ONLY;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.max_tasks_per_period.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("max", "period");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof MaxTasksPerPeriodParams p)) {
            return;
        }
        FamilySupport.validatePeriod(ctx, constraint, p.period(), true);
        if (p.max() < 0) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.max"),
                    "params.max must be >= 0");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof MaxTasksPerPeriodParams p) || p.period() == null) {
            return ModelCost.ZERO;
        }
        try {
            int buckets = PeriodBuckets.tile(index.horizon(), ZoneId.of("UTC"), p.period())[0].length;
            long taskBuckets = (long) index.resolveTasks(constraint.scope()).length * buckets;
            return new ModelCost(taskBuckets, 0, taskBuckets * 3 + buckets, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            return ModelCost.ZERO;
        }
    }

    @Override
    public ConstraintChecker checker() {
        return (ctx, constraint, params, sink) -> {
            MaxTasksPerPeriodParams p = (MaxTasksPerPeriodParams) params;
            int[][] tiles = PeriodBuckets.tile(ctx.index().horizon(), FamilySupport.zone(ctx.problem()), p.period());
            int[] lo = tiles[0];
            int[] hi = tiles[1];
            int[] counts = new int[lo.length];
            for (int t : ctx.index().resolveTasks(constraint.scope())) {
                if (ctx.solution().selected(t)) {
                    int bucket = PeriodBuckets.bucketOf(lo, hi, ctx.solution().start(t));
                    if (bucket >= 0) {
                        counts[bucket]++;
                    }
                }
            }
            for (int bucket = 0; bucket < counts.length; bucket++) {
                int excess = counts[bucket] - p.max();
                if (excess > 0) {
                    sink.violation(null, null, CODE,
                            "period starting at " + ctx.index().horizon().toInstant(lo[bucket])
                                    + " has " + counts[bucket] + " tasks (maximum " + p.max() + ")", excess);
                }
            }
        };
    }
}

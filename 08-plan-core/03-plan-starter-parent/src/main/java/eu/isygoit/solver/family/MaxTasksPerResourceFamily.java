package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.MaxTasksPerResourceParams;
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

import java.util.List;
import java.util.Set;

/**
 * Constraint family {@code max_tasks_per_resource} (design 4.3, 6.5.15): the number of tasks a resource takes, over the whole
 * horizon ({@code period == null}) or per period (task attributed to the period of its START), must be {@code <= max}
 * and/or {@code >= min}; {@code min} applies to resources that have candidates (and, per period, to the periods they can
 * start in).
 * <p>
 * Scope selects RESOURCES. Supports HARD, SOFT, RELAXABLE. Soft unit = tasks above max plus tasks below min, one violation
 * per (resource, period). Same implementation as {@code max_load_per_period} with metric COUNT. Stateless, thread-safe.
 */
@Component
public class MaxTasksPerResourceFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "max_tasks_per_resource";
    /** Violation code for an excess. */
    public static final String CODE_MAX = "MAX_TASKS_EXCEEDED";
    /** Violation code for a deficit. */
    public static final String CODE_MIN = "MIN_TASKS_NOT_REACHED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MaxTasksPerResourceParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.max_tasks_per_resource.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("max", "min", "period");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        MaxTasksPerResourceParams p = (MaxTasksPerResourceParams) params;
        if (p == null) {
            return;
        }
        FamilySupport.validatePeriod(ctx, constraint, p.period(), false);
        MaxLoadPerPeriodFamily.validateBounds(ctx, constraint, p.max(), p.min());
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        MaxTasksPerResourceParams p = (MaxTasksPerResourceParams) params;
        if (p == null) {
            return ModelCost.ZERO;
        }
        int[][] tiles;
        if (p.period() == null) {
            tiles = new int[][]{{0}, {index.horizonTicks()}};
        } else {
            try {
                tiles = PeriodBuckets.tile(index.horizon(), java.time.ZoneId.of("UTC"), p.period());
            } catch (IllegalArgumentException e) {
                return ModelCost.ZERO;
            }
        }
        return MaxLoadPerPeriodFamily.windowedCost(index, constraint, tiles);
    }

    @Override
    public ConstraintChecker checker() {
        return (CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) -> {
            MaxTasksPerResourceParams p = (MaxTasksPerResourceParams) params;
            PeriodLoadChecks.check(ctx, ctx.index().resolveResources(constraint.scope()), p.period(), p.period() == null,
                    LoadMetric.COUNT, p.max(), p.min(), sink, CODE_MAX, CODE_MIN);
        };
    }
}

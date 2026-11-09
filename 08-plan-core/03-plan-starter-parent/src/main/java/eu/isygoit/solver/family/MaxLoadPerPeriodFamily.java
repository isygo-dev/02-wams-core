package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.MaxLoadPerPeriodParams;
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
 * Constraint family {@code max_load_per_period} (design 4.3, 6.5.9): per resource and period the load, measured in
 * ticks (sum of task durations) or in task count, must be {@code <= max} and/or {@code >= min}. A task is attributed to the
 * period of its START (D-14); {@code min} applies to the periods some candidate of the resource can start in.
 * <p>
 * Scope selects RESOURCES. Supports HARD, SOFT, RELAXABLE. Soft unit = ticks (or tasks) above max plus ticks (or tasks) below
 * min, one violation per (resource, period). Stateless, thread-safe; ticks.
 */
@Component
public class MaxLoadPerPeriodFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "max_load_per_period";
    /** Violation code for an excess. */
    public static final String CODE_MAX = "MAX_LOAD_EXCEEDED";
    /** Violation code for a deficit. */
    public static final String CODE_MIN = "MIN_LOAD_NOT_REACHED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MaxLoadPerPeriodParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.max_load_per_period.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("period", "metric", "max", "min");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        MaxLoadPerPeriodParams p = (MaxLoadPerPeriodParams) params;
        if (p == null) {
            return;
        }
        FamilySupport.validatePeriod(ctx, constraint, p.period(), true);
        validateBounds(ctx, constraint, p.max(), p.min());
    }

    /** Shared bound validation of the period-load families. */
    static void validateBounds(ValidationContext ctx, Constraint constraint, Integer max, Integer min) {
        if (max == null && min == null) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params"),
                    "at least one of params.max and params.min is required");
        }
        if (max != null && max < 0) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.max"), "params.max must be >= 0");
        }
        if (min != null && min < 0) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.min"), "params.min must be >= 0");
        }
        if (max != null && min != null && min > max) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.min"),
                    "params.min must not exceed params.max");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        MaxLoadPerPeriodParams p = (MaxLoadPerPeriodParams) params;
        if (p == null || p.period() == null || p.period().unit() == null) {
            return ModelCost.ZERO;
        }
        int[][] tiles;
        try {
            tiles = PeriodBuckets.tile(index.horizon(), java.time.ZoneId.of("UTC"), p.period());
        } catch (IllegalArgumentException e) {
            return ModelCost.ZERO;
        }
        return windowedCost(index, constraint, tiles);
    }

    /** Size model shared by the period-load families. */
    static ModelCost windowedCost(CandidateIndexView index, Constraint constraint, int[][] tiles) {
        long[] size = FamilySupport.windowedSize(index, index.resolveResources(constraint.scope()), tiles[0], tiles[1]);
        return new ModelCost(size[0], size[1], 3 * size[0] + 2 * size[1], 0, 0, 0);
    }

    @Override
    public ConstraintChecker checker() {
        return (CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) -> {
            MaxLoadPerPeriodParams p = (MaxLoadPerPeriodParams) params;
            PeriodLoadChecks.check(ctx, ctx.index().resolveResources(constraint.scope()), p.period(), false, p.metric(),
                    p.max(), p.min(), sink, CODE_MAX, CODE_MIN);
        };
    }
}

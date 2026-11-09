package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.MaxConsecutiveParams;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PeriodBuckets;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * Constraint family {@code max_consecutive} (design 4.3, 6.5.7): a resource works at most {@code max} consecutive periods
 * (DAY / WEEK / TICKS buckets; a task belongs to the bucket of its START, D-14). A period is "worked" when the resource
 * has at least one assigned task starting in it.
 * <p>
 * Scope selects RESOURCES. Supports HARD, SOFT and RELAXABLE. Soft unit = ONE window of {@code max + 1} consecutive worked
 * periods (a run of L worked periods contains {@code L - max} such windows). Stateless, thread-safe; periods are tiled with
 * {@link PeriodBuckets} in {@code meta.timezone}.
 */
@Component
public class MaxConsecutiveFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "max_consecutive";
    /** Violation code reported by the checker. */
    public static final String CODE = "MAX_CONSECUTIVE_EXCEEDED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MaxConsecutiveParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.max_consecutive.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("period", "max");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        MaxConsecutiveParams p = (MaxConsecutiveParams) params;
        if (p == null) {
            return;
        }
        FamilySupport.validatePeriod(ctx, constraint, p.period(), true);
        if (p.max() < 1) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.max"),
                    "params.max must be >= 1 (number of consecutive periods allowed)");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        MaxConsecutiveParams p = (MaxConsecutiveParams) params;
        if (p == null || p.period() == null || p.period().unit() == null) {
            return ModelCost.ZERO;
        }
        int[][] tiles;
        try {
            tiles = PeriodBuckets.tile(index.horizon(), java.time.ZoneId.of("UTC"), p.period());
        } catch (IllegalArgumentException e) {
            return ModelCost.ZERO;
        }
        long[] size = FamilySupport.windowedSize(index, index.resolveResources(constraint.scope()), tiles[0], tiles[1]);
        long windows = Math.max(0, size[1]);
        return new ModelCost(size[0] + size[1] + windows, 0, 2 * size[0] + 2 * size[1] + windows, 0, 0, 0);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    /** Independent checker: counts every window of {@code max + 1} consecutive worked periods. */
    static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            MaxConsecutiveParams p = (MaxConsecutiveParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            PeriodSpec period = p.period();
            int[][] tiles = PeriodBuckets.tile(index.horizon(), FamilySupport.zone(ctx.problem()), period);
            int[] lo = tiles[0];
            int[] hi = tiles[1];
            int max = p.max();
            for (int r : index.resolveResources(constraint.scope())) {
                BitSet worked = new BitSet(lo.length);
                for (int t : solution.tasksOfResource(r)) {
                    int k = PeriodBuckets.bucketOf(lo, hi, solution.start(t));
                    if (k >= 0) {
                        worked.set(k);
                    }
                }
                int k = worked.nextSetBit(0);
                while (k >= 0) {
                    int end = worked.nextClearBit(k);
                    int length = end - k;
                    for (int w = k; w + max < end; w++) {
                        sink.violation(null, index.resourceId(r), CODE,
                                "resource " + index.resourceId(r) + " works " + (max + 1) + " consecutive periods from "
                                        + index.horizon().toInstant(lo[w]) + " (maximum " + max + ", run of " + length + ")",
                                1);
                    }
                    k = worked.nextSetBit(end);
                }
            }
        }
    }
}

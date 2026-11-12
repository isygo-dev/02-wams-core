package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.MaxWorkedPeriodsParams;
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

import java.time.ZoneId;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

@Component
public class MaxWorkedPeriodsFamily implements ConstraintFamily {

    public static final String TYPE = "max_worked_periods";
    public static final String CODE = "MAX_WORKED_PERIODS_EXCEEDED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MaxWorkedPeriodsParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.HARD_ONLY;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.max_worked_periods.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("max", "period");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof MaxWorkedPeriodsParams p)) {
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
        if (!(params instanceof MaxWorkedPeriodsParams p) || p.period() == null) {
            return ModelCost.ZERO;
        }
        try {
            int buckets = PeriodBuckets.tile(index.horizon(), ZoneId.of("UTC"), p.period())[0].length;
            long candidates = index.candidateCount();
            return new ModelCost(candidates * buckets + buckets, 0,
                    candidates * buckets * 2 + buckets, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            return ModelCost.ZERO;
        }
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            MaxWorkedPeriodsParams p = (MaxWorkedPeriodsParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            int[][] tiles = PeriodBuckets.tile(index.horizon(), FamilySupport.zone(ctx.problem()), p.period());
            int[] lo = tiles[0];
            int[] hi = tiles[1];
            for (int r : index.resolveResources(constraint.scope())) {
                BitSet worked = new BitSet(lo.length);
                for (int t : solution.tasksOfResource(r)) {
                    int bucket = PeriodBuckets.bucketOf(lo, hi, solution.start(t));
                    if (bucket >= 0) {
                        worked.set(bucket);
                    }
                }
                int excess = worked.cardinality() - p.max();
                if (excess > 0) {
                    sink.violation(null, index.resourceId(r), CODE,
                            "resource " + index.resourceId(r) + " works in " + worked.cardinality()
                                    + " periods (maximum " + p.max() + ")", excess);
                }
            }
        }
    }
}

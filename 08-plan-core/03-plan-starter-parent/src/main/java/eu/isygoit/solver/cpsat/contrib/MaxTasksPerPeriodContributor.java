package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.params.MaxTasksPerPeriodParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.BucketGrid;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.MaxTasksPerPeriodFamily;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MaxTasksPerPeriodContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MaxTasksPerPeriodFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MaxTasksPerPeriodParams p = (MaxTasksPerPeriodParams) constraint.params();
        BucketGrid grid = ctx.buckets(p.period());
        List<BoolVar>[] byBucket = new List[grid.size()];
        for (int k = 0; k < byBucket.length; k++) {
            byBucket[k] = new ArrayList<>();
        }
        int variables = 0;
        for (int t : ctx.index().resolveTasks(constraint.constraint().scope())) {
            Literal[] buckets = ctx.startBucket(t, grid);
            if (buckets == null) {
                continue;
            }
            for (int k = 0; k < buckets.length; k++) {
                if (buckets[k] == null) {
                    continue;
                }
                BoolVar startsHere = ctx.model().newBoolVar("");
                Literal presence = ctx.presence(t);
                ctx.model().addImplication(startsHere, presence);
                ctx.model().addImplication(startsHere, buckets[k]);
                ctx.model().addBoolOr(new Literal[]{presence.not(), buckets[k].not(), startsHere});
                byBucket[k].add(startsHere);
                variables++;
            }
        }
        int constraints = 0;
        for (List<BoolVar> terms : byBucket) {
            if (!terms.isEmpty()) {
                ContribSupport.enforce(ctx.model().addLessOrEqual(
                        LinearExpr.sum(terms.toArray(new BoolVar[0])), p.max()), enforcer.instance());
                constraints++;
            }
        }
        ContribSupport.count(ctx, variables, 0, variables * 3L + constraints, 0);
    }
}

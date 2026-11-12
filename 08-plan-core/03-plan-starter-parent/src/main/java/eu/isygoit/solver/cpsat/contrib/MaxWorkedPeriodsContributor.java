package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.params.MaxWorkedPeriodsParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.BucketGrid;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.MaxWorkedPeriodsFamily;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MaxWorkedPeriodsContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MaxWorkedPeriodsFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MaxWorkedPeriodsParams p = (MaxWorkedPeriodsParams) constraint.params();
        BucketGrid grid = ctx.buckets(p.period());
        CandidateIndex index = ctx.index();
        List<Literal>[][] products = new List[index.resourceCount()][grid.size()];
        for (int r : index.resolveResources(constraint.constraint().scope())) {
            for (int k = 0; k < grid.size(); k++) {
                products[r][k] = new ArrayList<>();
            }
        }
        int productCount = 0;
        for (int c = 0; c < index.candidateCount(); c++) {
            int r = index.candidateResource(c);
            if (products[r] == null) {
                continue;
            }
            int t = index.candidateTask(c);
            Literal[] starts = ctx.startBucket(t, grid);
            if (starts == null) {
                continue;
            }
            for (int k = 0; k < starts.length; k++) {
                if (starts[k] != null) {
                    Literal product = ctx.and(ctx.cand(c), starts[k]);
                    products[r][k].add(product);
                    productCount++;
                }
            }
        }
        int workedCount = 0;
        int constraints = 0;
        for (int r : index.resolveResources(constraint.constraint().scope())) {
            List<BoolVar> worked = new ArrayList<>();
            for (int k = 0; k < grid.size(); k++) {
                List<Literal> terms = products[r][k];
                if (terms == null || terms.isEmpty()) {
                    continue;
                }
                BoolVar isWorked = ctx.model().newBoolVar("");
                for (Literal term : terms) {
                    ctx.model().addImplication(term, isWorked);
                }
                List<Literal> definition = new ArrayList<>(terms.size() + 1);
                definition.add(isWorked.not());
                definition.addAll(terms);
                ctx.model().addBoolOr(definition);
                worked.add(isWorked);
                workedCount++;
            }
            if (!worked.isEmpty()) {
                ContribSupport.enforce(ctx.model().addLessOrEqual(
                        LinearExpr.sum(worked.toArray(new BoolVar[0])), p.max()),
                        enforcer.instance(-1, r));
                constraints++;
            }
        }
        ContribSupport.count(ctx, productCount + workedCount, 0,
                productCount * 2L + workedCount * 2L + constraints, 0);
    }
}

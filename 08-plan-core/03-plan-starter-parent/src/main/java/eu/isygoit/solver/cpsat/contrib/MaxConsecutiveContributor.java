package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.MaxConsecutiveParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.BucketGrid;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.family.MaxConsecutiveFamily;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.spi.PeriodBuckets;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;

/**
 * CP-SAT formulation of {@code max_consecutive} (design 6.5.7). Per scoped resource a literal {@code worked[k]} is the exact
 * OR of "candidate assigned and task starts in bucket k"; for every window of {@code max + 1} consecutive buckets that can
 * all be worked, the clause "not all worked" is enforced by the instance literal of the (resource, window): HARD
 * unconditional, RELAXABLE the shared active literal, SOFT one fresh satisfied literal per window (unit = one window, the
 * checker's unit). Windows containing an unreachable bucket are skipped (they can never be fully worked). Stateless,
 * thread-safe; buckets from {@code ctx.buckets(period)} (DST aware).
 */
@Component
public class MaxConsecutiveContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MaxConsecutiveFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MaxConsecutiveParams p = (MaxConsecutiveParams) constraint.params();
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        BucketGrid grid = ctx.buckets(p.period());
        int buckets = grid.size();
        int max = p.max();
        if (buckets <= max) {
            return;
        }
        int[][] byResource = FamilySupport.candidatesByResource(index);
        for (int r : index.resolveResources(constraint.constraint().scope())) {
            ctx.checkpoint();
            Map<Integer, BoolVar> worked = new TreeMap<>();
            Map<Integer, List<Literal>> definitions = new TreeMap<>();
            for (int c : byResource[r]) {
                int t = index.candidateTask(c);
                int[] range = PeriodBuckets.bucketRange(grid.lo(), grid.hi(), index.estStart(t), index.lstStart(t));
                Literal[] startBucket = range == null ? null : ctx.startBucket(t, grid);
                if (startBucket == null) {
                    continue;
                }
                for (int k = range[0]; k <= range[1]; k++) {
                    if (startBucket[k] == null) {
                        continue;
                    }
                    Literal product = ctx.and(ctx.cand(c), startBucket[k]);
                    BoolVar w = worked.computeIfAbsent(k, x -> model.newBoolVar(""));
                    model.addImplication(product, w);
                    definitions.computeIfAbsent(k, x -> new ArrayList<>()).add(product);
                }
            }
            for (Map.Entry<Integer, BoolVar> e : worked.entrySet()) {
                List<Literal> clause = new ArrayList<>();
                clause.add(e.getValue().not());
                clause.addAll(definitions.get(e.getKey()));
                model.addBoolOr(clause);
            }
            ContribSupport.count(ctx, worked.size(), 0, 2L * worked.size(), 0);
            for (int k = 0; k + max < buckets; k++) {
                List<Literal> negations = new ArrayList<>(max + 1);
                for (int j = k; j <= k + max; j++) {
                    BoolVar w = worked.get(j);
                    if (w == null) {
                        negations = null;
                        break;
                    }
                    negations.add(w.not());
                }
                if (negations == null) {
                    continue;
                }
                Literal instance = enforcer.instance(-1, r);
                ContribSupport.enforce(model.addBoolOr(negations), instance);
                ContribSupport.count(ctx, 0, 0, 1, 0);
            }
        }
    }
}

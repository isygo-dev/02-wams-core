package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.BucketGrid;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.spi.PeriodBuckets;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shared CP-SAT formulation of {@code max_load_per_period} and {@code max_tasks_per_resource} (design 6.5.9, 6.5.15): for
 * every scoped resource and applicable bucket (see {@link FamilySupport#reachableBuckets}, the same set the checker uses) the
 * load {@code sum value_c * (cand_c and startsInBucket_c)} is bounded by {@code max} and {@code min}.
 * <p>
 * Enforcement (Universal contributor rule): HARD/RELAXABLE/explain enforce a linear bound with the instance literal of the
 * (resource); SOFT penalises the integer excess / deficit through {@link Enforcer#penalizeMeasure} (unit = ticks or tasks,
 * the checker's measure). Stateless; used only on the building thread. Units: ticks; the value of a candidate is its duration
 * (TICKS) or 1 (COUNT).
 */
final class PeriodLoadEmitter {

    private record Term(Literal literal, long value) {
    }

    private PeriodLoadEmitter() {
    }

    /**
     * Emits the constraint.
     *
     * @param period null = one bucket covering the whole horizon (no start-bucket literals needed)
     */
    static void emit(CpModelContext ctx, CompiledConstraint compiled, Enforcer enforcer, PeriodSpec period,
                     LoadMetric metric, Integer max, Integer min) {
        CandidateIndex index = ctx.index();
        boolean whole = period == null;
        BucketGrid grid = whole ? null : ctx.buckets(period);
        int[] lo = whole ? new int[]{0} : grid.lo();
        int[] hi = whole ? new int[]{index.horizonTicks()} : grid.hi();
        int[] resources = index.resolveResources(compiled.constraint().scope());
        Map<Integer, BitSet> reach = FamilySupport.reachableBuckets(index, resources, lo, hi);
        int[][] byResource = FamilySupport.candidatesByResource(index);

        for (Map.Entry<Integer, BitSet> entry : reach.entrySet()) {
            ctx.checkpoint();
            int r = entry.getKey();
            Map<Integer, List<Term>> perBucket = new TreeMap<>();
            for (int c : byResource[r]) {
                int t = index.candidateTask(c);
                int[] range = PeriodBuckets.bucketRange(lo, hi, index.estStart(t), index.lstStart(t));
                if (range == null) {
                    continue;
                }
                Literal[] startBucket = whole ? null : ctx.startBucket(t, grid);
                if (!whole && startBucket == null) {
                    continue;
                }
                long value = metric == LoadMetric.COUNT ? 1L : index.candidateDuration(c);
                for (int k = range[0]; k <= range[1]; k++) {
                    Literal literal;
                    if (whole) {
                        literal = ctx.cand(c);
                    } else if (startBucket[k] == null) {
                        continue;
                    } else {
                        literal = ctx.and(ctx.cand(c), startBucket[k]);
                    }
                    perBucket.computeIfAbsent(k, x -> new ArrayList<>()).add(new Term(literal, value));
                }
            }
            BitSet buckets = entry.getValue();
            for (int k = buckets.nextSetBit(0); k >= 0; k = buckets.nextSetBit(k + 1)) {
                List<Term> terms = perBucket.getOrDefault(k, List.of());
                long total = 0;
                for (Term term : terms) {
                    total += term.value();
                }
                if (max != null && total > max) {
                    emitMax(ctx, enforcer, r, terms, total, max);
                }
                if (min != null && min > 0) {
                    emitMin(ctx, enforcer, r, terms, min);
                }
            }
        }
    }

    private static void emitMax(CpModelContext ctx, Enforcer enforcer, int r, List<Term> terms, long total, long max) {
        if (enforcer.mode() == Enforcer.Mode.SOFT) {
            long maxExcess = total - max;
            IntVar excess = ctx.model().newIntVar(0, maxExcess, "");
            if (enforcer.penalizeMeasure(excess.build(), maxExcess, -1)) {
                LinearExprBuilder b = LinearExpr.newBuilder().add(excess);
                for (Term term : terms) {
                    b.addTerm(term.literal(), -term.value());
                }
                ctx.model().addGreaterOrEqual(b.build(), -max);
                ContribSupport.count(ctx, 0, 1, 1, 0);
                return;
            }
        }
        Literal instance = enforcer.instance(-1, r);
        ContribSupport.enforce(ctx.model().addLessOrEqual(load(terms), max), instance);
        ContribSupport.count(ctx, 0, 0, 1, 0);
    }

    private static void emitMin(CpModelContext ctx, Enforcer enforcer, int r, List<Term> terms, long min) {
        if (enforcer.mode() == Enforcer.Mode.SOFT) {
            IntVar deficit = ctx.model().newIntVar(0, min, "");
            if (enforcer.penalizeMeasure(deficit.build(), min, -1)) {
                LinearExprBuilder b = LinearExpr.newBuilder().add(deficit);
                for (Term term : terms) {
                    b.addTerm(term.literal(), term.value());
                }
                ctx.model().addGreaterOrEqual(b.build(), min);
                ContribSupport.count(ctx, 0, 1, 1, 0);
                return;
            }
        }
        Literal instance = enforcer.instance(-1, r);
        ContribSupport.enforce(ctx.model().addGreaterOrEqual(load(terms), min), instance);
        ContribSupport.count(ctx, 0, 0, 1, 0);
    }

    private static LinearExpr load(List<Term> terms) {
        LinearExprBuilder b = LinearExpr.newBuilder();
        for (Term term : terms) {
            b.addTerm(term.literal(), term.value());
        }
        return b.build();
    }
}

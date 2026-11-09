package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntervalVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.MinRestParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.family.MinRestFamily;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CP-SAT formulation of {@code min_rest} (design 6.5.8) on unary resources, following the Universal contributor rule.
 * <p>
 * HARD in the solve pass ({@code enforcer.instance() == null}): a separate {@code NoOverlap} over "inflated" optional
 * intervals {@code [start, start + duration + rest)}; when EVERY candidate of the resource has a fixed start (pinned
 * roster shifts) no interval is created at all: the conflicting pairs are decided at build time and become clauses
 * {@code (not cand_i or not cand_j)} (static-pair shortcut, O(conflicting pairs)). Otherwise (RELAXABLE, SOFT, explain pass):
 * pairwise literals for the pairs whose start domains can conflict, ONE instance literal per pair (soft unit = one pair,
 * the checker's unit), with static pairs again reduced to a clause; guarded by {@code max-pairwise}. The afterTasks filter
 * decides which tasks are inflated; candidates of inflated size 0 never conflict. Stateless, thread-safe; ticks.
 */
@Component
public class MinRestContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MinRestFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MinRestParams p = (MinRestParams) constraint.params();
        int rest = p.minRestTicks();
        if (rest <= 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        boolean[] after = p.afterTasks() == null ? null
                : FamilySupport.flags(index.taskCount(), index.resolveTasks(p.afterTasks()));
        boolean global = enforcer.mode() == Enforcer.Mode.HARD && enforcer.instance() == null;
        long limit = index.limits().getMaxPairwise();
        long used = 0;
        int[][] byResource = FamilySupport.candidatesByResource(index);
        for (int r : index.resolveResources(constraint.constraint().scope())) {
            ctx.checkpoint();
            int[] cands = byResource[r];
            if (cands.length < 2 || index.capacity(r) != 1) {
                continue;
            }
            if (global && !allStatic(index, cands, after, rest)) {
                buildNoOverlap(ctx, model, index, cands, after, rest);
                continue;
            }
            int[] pairs = FamilySupport.conflictPairs(index, cands, after, rest, limit - used);
            used += pairs.length / 2;
            if (!ContribSupport.pairwiseOk(ctx, used)) {
                return;
            }
            for (int k = 0; k < pairs.length; k += 2) {
                if (k % 2048 == 0) {
                    ctx.checkpoint();
                }
                emitPair(ctx, model, index, enforcer, r, pairs[k], pairs[k + 1], after, rest);
            }
            ContribSupport.count(ctx, 3L * pairs.length / 2, 0, 2L * pairs.length / 2, 0);
        }
    }

    private static boolean allStatic(CandidateIndex index, int[] cands, boolean[] after, int rest) {
        for (int c : cands) {
            int t = index.candidateTask(c);
            if (size(index, c, after, rest) > 0 && index.estStart(t) != index.lstStart(t)) {
                return false;
            }
        }
        return true;
    }

    private static int size(CandidateIndex index, int c, boolean[] after, int rest) {
        int t = index.candidateTask(c);
        return index.candidateDuration(c) + (after == null || after[t] ? rest : 0);
    }

    private static void buildNoOverlap(CpModelContext ctx, CpModel model, CandidateIndex index, int[] cands,
                                       boolean[] after, int rest) {
        List<IntervalVar> intervals = new ArrayList<>(cands.length);
        for (int c : cands) {
            int size = size(index, c, after, rest);
            if (size > 0) {
                intervals.add(model.newOptionalFixedSizeIntervalVar(
                        ctx.start(index.candidateTask(c)), size, ctx.cand(c), ""));
            }
        }
        if (intervals.size() > 1) {
            model.addNoOverlap(intervals);
        }
        ContribSupport.count(ctx, 0, 0, 1, intervals.size());
    }

    private static void emitPair(CpModelContext ctx, CpModel model, CandidateIndex index, Enforcer enforcer, int r,
                                 int ci, int cj, boolean[] after, int rest) {
        int ti = index.candidateTask(ci);
        int tj = index.candidateTask(cj);
        Literal instance = enforcer.instance(ti, r);
        boolean fixed = index.estStart(ti) == index.lstStart(ti) && index.estStart(tj) == index.lstStart(tj);
        if (fixed) {
            ContribSupport.enforce(model.addBoolOr(new Literal[]{ctx.cand(ci).not(), ctx.cand(cj).not()}), instance);
            return;
        }
        BoolVar first = model.newBoolVar("");
        BoolVar second = model.newBoolVar("");
        LinearExpr endI = ContribSupport.endOf(ctx, ti, size(index, ci, after, rest));
        LinearExpr endJ = ContribSupport.endOf(ctx, tj, size(index, cj, after, rest));
        model.addLessOrEqual(endI, ctx.start(tj)).onlyEnforceIf(first);
        model.addLessOrEqual(endJ, ctx.start(ti)).onlyEnforceIf(second);
        ContribSupport.enforce(model.addBoolOr(new Literal[]{first, second, ctx.cand(ci).not(), ctx.cand(cj).not()}),
                instance);
    }
}

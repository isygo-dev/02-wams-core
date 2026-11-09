package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.ForbiddenPairParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.family.ForbiddenPairFamily;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CP-SAT formulation of {@code forbidden_pair} (design 6.5.14). TASK_TASK / SAME_RESOURCE: one instance literal per pair
 * guards the clauses {@code (not a_on_r or not b_on_r)} of every common resource (soft unit = one pair). TASK_TASK /
 * TIME_OVERLAP: the order-literal clause of {@code no_overlap} with gap 0 for the explicit pair (the explicit pair list is
 * small, so the pairwise form is used for every enforcement mode). RESOURCE_RESOURCE: per task with candidates on both
 * resources, one instance literal and the clause {@code (not on_r1 or not on_r2)}. Stateless, thread-safe; ticks.
 */
@Component
public class ForbiddenPairContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return ForbiddenPairFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        ForbiddenPairParams p = (ForbiddenPairParams) constraint.params();
        CandidateIndex index = ctx.index();
        if (p.kind() == ForbiddenPairParams.Kind.RESOURCE_RESOURCE) {
            resourcePairs(ctx, index, enforcer, p);
            return;
        }
        boolean overlap = p.relation() == ForbiddenPairParams.Relation.TIME_OVERLAP;
        for (ForbiddenPairParams.IdPair pair : p.pairs()) {
            ctx.checkpoint();
            int a = index.taskIdx(pair.a());
            int b = index.taskIdx(pair.b());
            if (a < 0 || b < 0 || a == b) {
                continue;
            }
            if (overlap) {
                timeOverlap(ctx, enforcer, a, b);
            } else {
                sameResource(ctx, index, enforcer, a, b);
            }
        }
    }

    private static void timeOverlap(CpModelContext ctx, Enforcer enforcer, int a, int b) {
        CpModel model = ctx.model();
        Literal instance = enforcer.instance(a, -1);
        BoolVar first = model.newBoolVar("");
        BoolVar second = model.newBoolVar("");
        model.addLessOrEqual(ctx.end(a), ctx.start(b)).onlyEnforceIf(first);
        model.addLessOrEqual(ctx.end(b), ctx.start(a)).onlyEnforceIf(second);
        ContribSupport.enforce(model.addBoolOr(new Literal[]{first, second, ctx.presence(a).not(), ctx.presence(b).not()}),
                instance);
        ContribSupport.count(ctx, 2, 0, 3, 0);
    }

    private static void sameResource(CpModelContext ctx, CandidateIndex index, Enforcer enforcer, int a, int b) {
        Map<Integer, List<Literal>> onA = ContribSupport.literalsByResource(ctx, index, a, null);
        Map<Integer, List<Literal>> onB = ContribSupport.literalsByResource(ctx, index, b, null);
        List<Literal[]> clauses = new ArrayList<>();
        for (Map.Entry<Integer, List<Literal>> e : onA.entrySet()) {
            List<Literal> other = onB.get(e.getKey());
            if (other == null) {
                continue;
            }
            for (Literal ca : e.getValue()) {
                for (Literal cb : other) {
                    clauses.add(new Literal[]{ca.not(), cb.not()});
                }
            }
        }
        if (clauses.isEmpty()) {
            return;
        }
        Literal instance = enforcer.instance(a, -1);
        for (Literal[] clause : clauses) {
            ContribSupport.enforce(ctx.model().addBoolOr(clause), instance);
        }
        ContribSupport.count(ctx, 0, 0, clauses.size(), 0);
    }

    private static void resourcePairs(CpModelContext ctx, CandidateIndex index, Enforcer enforcer, ForbiddenPairParams p) {
        int[][] byResource = FamilySupport.candidatesByResource(index);
        for (ForbiddenPairParams.IdPair pair : p.pairs()) {
            ctx.checkpoint();
            int r1 = index.resourceIdx(pair.a());
            int r2 = index.resourceIdx(pair.b());
            if (r1 < 0 || r2 < 0 || r1 == r2) {
                continue;
            }
            Map<Integer, List<Integer>> onFirst = new TreeMap<>();
            for (int c : byResource[r1]) {
                onFirst.computeIfAbsent(index.candidateTask(c), x -> new ArrayList<>()).add(c);
            }
            Map<Integer, List<Integer>> onSecond = new TreeMap<>();
            for (int c : byResource[r2]) {
                onSecond.computeIfAbsent(index.candidateTask(c), x -> new ArrayList<>()).add(c);
            }
            for (Map.Entry<Integer, List<Integer>> e : onFirst.entrySet()) {
                List<Integer> other = onSecond.get(e.getKey());
                if (other == null) {
                    continue;
                }
                Literal instance = enforcer.instance(e.getKey(), -1);
                for (int c1 : e.getValue()) {
                    for (int c2 : other) {
                        ContribSupport.enforce(ctx.model().addBoolOr(
                                new Literal[]{ctx.cand(c1).not(), ctx.cand(c2).not()}), instance);
                    }
                }
                ContribSupport.count(ctx, 0, 0, (long) e.getValue().size() * other.size(), 0);
            }
        }
    }
}

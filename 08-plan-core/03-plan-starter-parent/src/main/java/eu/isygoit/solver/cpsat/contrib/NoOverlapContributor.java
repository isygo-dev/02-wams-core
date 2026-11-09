package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntervalVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.NoOverlapParams;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.NoOverlapFamily;
import eu.isygoit.solver.spi.CandidateIndexView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CP-SAT formulation of {@code no_overlap} (design 6.5.3), honouring the Universal contributor rule.
 * <ul>
 *   <li>HARD in the solve pass (no enforcement literal possible): one global {@code NoOverlap} per group over the master
 *   intervals (inflated optional intervals of size {@code duration + gap} when {@code minGapTicks > 0}); zero-size tasks
 *   are skipped.</li>
 *   <li>SOFT, RELAXABLE and the explain pass: pairwise fallback with order literals {@code o1: end_i + gap <= start_j},
 *   {@code o2: end_j + gap <= start_i} and the clause {@code o1 or o2 or not p_i or not p_j} enforced by
 *   {@code enforcer.instance(i, -1)}; SOFT unit = one overlapping pair. Pairs that the domains already separate get no
 *   literal. More than {@code maxPairwise} pairs is {@code PAIRWISE_TOO_LARGE} in the solve pass; the explain pass
 *   skips the constraint (it is then not part of the core).</li>
 * </ul>
 * Stateless and thread-safe; units are ticks.
 */
@Component
public class NoOverlapContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return NoOverlapFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        NoOverlapParams p = (NoOverlapParams) constraint.params();
        CandidateIndexView idx = ctx.index();
        int[][] groups = NoOverlapFamily.groups(idx, constraint.constraint(), p);
        boolean unconditional = enforcer.mode() == Enforcer.Mode.HARD && !ctx.explainMode();
        if (unconditional) {
            applyGlobal(ctx, groups, p.minGapTicks());
        } else {
            applyPairwise(ctx, constraint, enforcer, groups, p.minGapTicks());
        }
    }

    private static void applyGlobal(CpModelContext ctx, int[][] groups, int gap) {
        CpModel model = ctx.model();
        CandidateIndexView idx = ctx.index();
        int intervals = 0;
        int constraints = 0;
        for (int[] group : groups) {
            ctx.checkpoint();
            List<IntervalVar> list = new ArrayList<>();
            for (int t : group) {
                if (NoOverlapFamily.zeroSize(idx, t, gap)) {
                    continue;
                }
                if (gap == 0) {
                    list.add(ctx.masterInterval(t));
                } else {
                    // inflated interval [start, end + gap): the standard minimum-gap trick
                    LinearExpr size = LinearExpr.newBuilder().add(ctx.end(t)).addTerm(ctx.start(t), -1).add(gap).build();
                    list.add(model.newOptionalIntervalVar(ctx.start(t), size, plus(ctx.end(t), gap), ctx.presence(t), ""));
                    intervals++;
                }
            }
            if (list.size() >= 2) {
                model.addNoOverlap(list); // OR-TOOLS-VERIFY: addNoOverlap(Iterable<IntervalVar>) in 9.15
                constraints++;
            }
        }
        ctx.count(0, 0, constraints, intervals);
    }

    private static void applyPairwise(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer, int[][] groups, int gap) {
        CpModel model = ctx.model();
        CandidateIndexView idx = ctx.index();
        long maxPairwise = idx.limits().getMaxPairwise(); // needs from WP2: PlanLimitsProperties.maxPairwise
        long total = 0;
        for (int[] group : groups) {
            total += NoOverlapFamily.countConflictPairs(idx, group, gap);
        }
        if (total > maxPairwise) {
            if (ctx.explainMode()) {
                return; // skipped from the explanation (design 6.10)
            }
            throw new InvalidRequestException("PAIRWISE_TOO_LARGE", "no_overlap constraint '" + constraint.constraint().id()
                    + "' needs " + total + " pair literals, limit " + maxPairwise);
        }
        int iterations = 0;
        for (int[] group : groups) {
            long[] maxDur = new long[group.length];
            boolean[] skip = new boolean[group.length];
            for (int a = 0; a < group.length; a++) {
                maxDur[a] = NoOverlapFamily.maxDuration(idx, group[a]);
                skip[a] = gap == 0 && maxDur[a] == 0;
            }
            for (int a = 0; a < group.length; a++) {
                if (skip[a]) {
                    continue;
                }
                for (int b = a + 1; b < group.length; b++) {
                    if (++iterations % 1024 == 0) {
                        ctx.checkpoint();
                    }
                    if (skip[b] || !NoOverlapFamily.mayConflict(idx, group[a], group[b], gap, maxDur[a], maxDur[b])) {
                        continue;
                    }
                    int i = group[a];
                    int j = group[b];
                    Literal instance = enforcer.instance(i, -1); // ONE literal per pair
                    BoolVar iFirst = model.newBoolVar("");
                    BoolVar jFirst = model.newBoolVar("");
                    model.addLessOrEqual(plus(ctx.end(i), gap), ctx.start(j)).onlyEnforceIf(iFirst);
                    model.addLessOrEqual(plus(ctx.end(j), gap), ctx.start(i)).onlyEnforceIf(jFirst);
                    List<Literal> clause = new ArrayList<>();
                    clause.add(iFirst);
                    clause.add(jFirst);
                    addAbsence(ctx, clause, i);
                    addAbsence(ctx, clause, j);
                    com.google.ortools.sat.Constraint rule = model.addBoolOr(clause.toArray(new Literal[0]));
                    Literal[] conditions = enforcer.conditions(instance);
                    if (conditions.length > 0) {
                        rule.onlyEnforceIf(conditions);
                    }
                }
            }
        }
        ctx.count((int) Math.min(Integer.MAX_VALUE, 2 * total + (enforcer.mode() == Enforcer.Mode.SOFT ? total : 0)), 0,
                (int) Math.min(Integer.MAX_VALUE, 3 * total), 0);
    }

    private static void addAbsence(CpModelContext ctx, List<Literal> clause, int t) {
        Literal p = ctx.presence(t);
        if (p != ctx.trueLit()) {
            clause.add(p.not());
        }
    }

    /** @return {@code expr + constant} as a new expression. OR-TOOLS-VERIFY: LinearExprBuilder.add(LinearArgument) / add(long) */
    private static LinearExpr plus(LinearArgument expr, long constant) {
        return LinearExpr.newBuilder().add(expr).add(constant).build();
    }
}

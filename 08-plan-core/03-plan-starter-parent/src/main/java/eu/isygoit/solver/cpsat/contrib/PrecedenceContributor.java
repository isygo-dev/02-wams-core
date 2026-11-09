package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.params.PrecedenceParams;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.PrecedenceFamily;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * CP-SAT formulation of {@code precedence} (design 6.5.4). For every (from, to) pair the linear precedence
 * (FS / SS / FF / SF with {@code minLag} and optional {@code maxLag}, affine start / end expressions, no auxiliary
 * variable) is enforced by {@code enforcer.conditions(enforcer.instance(to, -1), p_from, p_to)}: all constraints of one
 * pair share ONE instance literal, so the SOFT unit is one violated pair whichever lag is broken. Linear constraints
 * accept enforcement literals, hence the same code serves HARD (unconditional apart from presence), SOFT, RELAXABLE and the
 * explain pass. The pair count is guarded by {@code maxPairwise} ({@code PAIRWISE_TOO_LARGE}; the explain pass skips the
 * family). The edge emission is local (not shared with the structural {@code Task.predecessors} code) so that this
 * contributor has no dependency on WP3 internals. Stateless and thread-safe; units are ticks.
 */
@Component
public class PrecedenceContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return PrecedenceFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        PrecedenceParams p = (PrecedenceParams) constraint.params();
        long pairs = PrecedenceFamily.pairCount(ctx.index(), constraint.constraint(), p);
        long maxPairwise = ctx.index().limits().getMaxPairwise(); // needs from WP2: PlanLimitsProperties.maxPairwise
        if (pairs > maxPairwise) {
            if (ctx.explainMode()) {
                return; // skipped from the explanation (design 6.10)
            }
            throw new InvalidRequestException("PAIRWISE_TOO_LARGE", "precedence constraint '" + constraint.constraint().id()
                    + "' expands to " + pairs + " pairs, limit " + maxPairwise);
        }
        AtomicInteger counter = new AtomicInteger();
        PrecedenceFamily.forEachPair(ctx.index(), constraint.constraint(), p, (from, to) -> {
            if (counter.incrementAndGet() % 1024 == 0) {
                ctx.checkpoint();
            }
            emit(ctx, enforcer, p, from, to);
        });
        ctx.count(0, 0, (int) Math.min(Integer.MAX_VALUE, pairs * (p.maxLag() == null ? 1 : 2)), 0);
    }

    private static void emit(CpModelContext ctx, Enforcer enforcer, PrecedenceParams p, int i, int j) {
        CpModel model = ctx.model();
        Literal[] conditions = enforcer.conditions(enforcer.instance(j, -1), optionalPresence(ctx, i), optionalPresence(ctx, j));
        LinearArgument lhs; // OR-TOOLS-VERIFY: LinearArgument is the common supertype of IntVar and LinearExpr
        LinearArgument rhs;
        switch (p.type()) {
            case START_START -> {
                lhs = ctx.start(j);
                rhs = ctx.start(i);
            }
            case FINISH_FINISH -> {
                lhs = ctx.end(j);
                rhs = ctx.end(i);
            }
            case START_FINISH -> {
                lhs = ctx.end(j);
                rhs = ctx.start(i);
            }
            default -> {
                lhs = ctx.start(j);
                rhs = ctx.end(i);
            }
        }
        enforce(model.addGreaterOrEqual(lhs, plus(rhs, p.minLag())), conditions);
        if (p.maxLag() != null) {
            enforce(model.addLessOrEqual(lhs, plus(rhs, p.maxLag())), conditions);
        }
    }

    private static Literal optionalPresence(CpModelContext ctx, int t) {
        Literal presence = ctx.presence(t);
        return presence == ctx.trueLit() ? null : presence;
    }

    private static void enforce(com.google.ortools.sat.Constraint constraint, Literal[] conditions) {
        if (conditions != null && conditions.length > 0) {
            constraint.onlyEnforceIf(conditions);
        }
    }

    /** @return {@code expr + constant}. OR-TOOLS-VERIFY: LinearExprBuilder.add(LinearArgument) / add(long) */
    private static LinearExpr plus(LinearArgument expr, long constant) {
        return LinearExpr.newBuilder().add(expr).add(constant).build();
    }
}

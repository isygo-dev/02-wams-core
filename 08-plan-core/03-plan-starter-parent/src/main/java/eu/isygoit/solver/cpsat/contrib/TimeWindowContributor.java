package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.TimeWindowParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.TimeWindowFamily;
import eu.isygoit.solver.spi.CandidateIndexView;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code time_window} (design 6.5.5).
 * <ul>
 *   <li>HARD solve: start bounds were applied by {@link TimeWindowFamily#preprocess}; the contributor only adds the exact
 *   end-bound constraints for tasks whose candidate durations differ (the preprocess uses the weakest duration).</li>
 *   <li>SOFT: per bound an integer slack {@code >= 0} with {@code slack + start >= earliestStart} (or
 *   {@code slack >= start - latestStart}, and the same on the end expression), conditioned on the task presence;
 *   {@code enforcer.penalizeMeasure(slack, horizonTicks, task)} charges weight x slack, so the model value is the total number
 *   of ticks outside the window, exactly what the checker measures.</li>
 *   <li>RELAXABLE and the explain pass: the plain bound constraint enforced by
 *   {@code enforcer.conditions(enforcer.instance(task, -1), p_task)}.</li>
 * </ul>
 * Stateless and thread-safe; units are ticks.
 */
@Component
public class TimeWindowContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return TimeWindowFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        TimeWindowParams p = (TimeWindowParams) constraint.params();
        CandidateIndexView idx = ctx.index();
        int[] bounds = TimeWindowFamily.bounds(idx.horizon(), p);
        boolean hardSolve = enforcer.mode() == Enforcer.Mode.HARD && !ctx.explainMode();
        long maxMeasure = idx.horizonTicks();
        int constraints = 0;
        int intVars = 0;
        int iterations = 0;
        for (int t : idx.resolveTasks(constraint.constraint().scope())) {
            if (++iterations % 1024 == 0) {
                ctx.checkpoint();
            }
            boolean variableDuration = variableDuration(idx, t);
            for (int which = 0; which < 4; which++) {
                int bound = bounds[which];
                if (bound == TimeWindowFamily.NONE) {
                    continue;
                }
                boolean endBound = which == TimeWindowFamily.EARLIEST_END || which == TimeWindowFamily.LATEST_END;
                if (hardSolve) {
                    if (!(endBound && variableDuration)) {
                        continue; // exact through the preprocess start-domain restriction
                    }
                    addHard(ctx, enforcer, t, which, bound);
                    constraints++;
                } else if (enforcer.mode() == Enforcer.Mode.SOFT) {
                    addSoft(ctx, enforcer, t, which, bound, maxMeasure);
                    constraints++;
                    intVars++;
                } else {
                    addHard(ctx, enforcer, t, which, bound);
                    constraints++;
                }
            }
        }
        ctx.count(0, intVars, constraints, 0);
    }

    private static void addHard(CpModelContext ctx, Enforcer enforcer, int t, int which, int bound) {
        CpModel model = ctx.model();
        Literal[] conditions = enforcer.conditions(enforcer.instance(t, -1), optionalPresence(ctx, t));
        com.google.ortools.sat.Constraint rule = switch (which) {
            case TimeWindowFamily.EARLIEST_START -> model.addGreaterOrEqual(ctx.start(t), bound);
            case TimeWindowFamily.LATEST_START -> model.addLessOrEqual(ctx.start(t), bound);
            case TimeWindowFamily.EARLIEST_END -> model.addGreaterOrEqual(ctx.end(t), bound);
            default -> model.addLessOrEqual(ctx.end(t), bound);
        };
        if (conditions.length > 0) {
            rule.onlyEnforceIf(conditions);
        }
    }

    private static void addSoft(CpModelContext ctx, Enforcer enforcer, int t, int which, int bound, long maxMeasure) {
        CpModel model = ctx.model();
        IntVar slack = model.newIntVar(0, maxMeasure, "");
        com.google.ortools.sat.Constraint channel = switch (which) {
            case TimeWindowFamily.EARLIEST_START -> model.addGreaterOrEqual(sum(slack, ctx.start(t)), bound);
            case TimeWindowFamily.LATEST_START -> model.addGreaterOrEqual(slack, plus(ctx.start(t), -(long) bound));
            case TimeWindowFamily.EARLIEST_END -> model.addGreaterOrEqual(sum(slack, ctx.end(t)), bound);
            default -> model.addGreaterOrEqual(slack, plus(ctx.end(t), -(long) bound));
        };
        // definitional channel of the measure (never enforced by the instance); an absent optional task owes nothing
        Literal presence = optionalPresence(ctx, t);
        if (presence != null) {
            channel.onlyEnforceIf(presence);
        }
        // OR-TOOLS-VERIFY: wrapping the IntVar in a LinearExpr (the Enforcer signature takes a LinearExpr)
        enforcer.penalizeMeasure(LinearExpr.newBuilder().add(slack).build(), maxMeasure, t);
    }

    private static boolean variableDuration(CandidateIndexView idx, int t) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int c = idx.candidateBegin(t); c < idx.candidateEnd(t); c++) {
            min = Math.min(min, idx.candidateDuration(c));
            max = Math.max(max, idx.candidateDuration(c));
        }
        return max != Long.MIN_VALUE && min != max;
    }

    private static Literal optionalPresence(CpModelContext ctx, int t) {
        Literal presence = ctx.presence(t);
        return presence == ctx.trueLit() ? null : presence;
    }

    /** @return {@code a + b}. OR-TOOLS-VERIFY: LinearExprBuilder.add(LinearArgument) */
    private static LinearExpr sum(LinearArgument a, LinearArgument b) {
        return LinearExpr.newBuilder().add(a).add(b).build();
    }

    /** @return {@code expr + constant}. OR-TOOLS-VERIFY: LinearExprBuilder.add(long) */
    private static LinearExpr plus(LinearArgument expr, long constant) {
        return LinearExpr.newBuilder().add(expr).add(constant).build();
    }
}

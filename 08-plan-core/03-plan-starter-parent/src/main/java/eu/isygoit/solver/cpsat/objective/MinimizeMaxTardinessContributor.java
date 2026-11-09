package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeMaxTardinessObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_max_tardiness} (design 4.4): one variable {@code T in [0, maxPossibleTardiness]}
 * with {@code T >= end_t - due_t} for every task of the scope that has a due date (enforced by presence for optional
 * tasks); the term is {@code weight * T}, bounds {@code [0, weight * maxPossibleTardiness]}. Ticks, never scaled.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeMaxTardinessContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeMaxTardinessObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        int[] tasks = index.resolveTasks(objective.objective().scope());
        long hi = 0;
        for (int i = 0; i < tasks.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int t = tasks[i];
            if (index.due(t) != ObjectiveMath.NO_DUE) {
                hi = Math.max(hi, Math.min((long) index.latestEnd(t), index.horizonTicks()) - index.due(t));
            }
        }
        if (hi <= 0) {
            return;
        }
        long coef = ObjectiveTerms.coef(weight, 1, "minimize_max_tardiness");
        IntVar max = ctx.newTierVar("maxtard_" + objective.index(), 0, hi);
        int constraints = 0;
        for (int i = 0; i < tasks.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int t = tasks[i];
            int due = index.due(t);
            if (due == ObjectiveMath.NO_DUE) {
                continue;
            }
            LinearExpr late = LinearExpr.newBuilder().add(ctx.end(t)).add(-(long) due).build();
            Constraint c = ctx.model().addGreaterOrEqual(max, late);
            if (!index.required(t)) {
                c.onlyEnforceIf(ctx.presence(t));
            }
            constraints++;
        }
        ctx.count(0, 0, constraints, 0);
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        acc.add(max, coef, 0, ScaledMath.mulExact(coef, hi));
        acc.flush(sink, objective.tier(), "minimize_max_tardiness");
    }
}

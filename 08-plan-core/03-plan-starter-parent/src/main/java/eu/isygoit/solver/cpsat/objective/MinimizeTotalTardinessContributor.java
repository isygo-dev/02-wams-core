package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.params.TardinessParams;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeTotalTardinessObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_total_tardiness} (design 4.4): per task with a due date a variable
 * {@code tard_t in [0, latestEnd - due]} with {@code tard_t >= end_t - due} (enforced by presence for optional tasks);
 * tasks that can never be tardy are skipped. The term is {@code sum weight * (1 + priority if weightBy=PRIORITY) *
 * tard_t}, bounds {@code [0, sum coef_t * maxTard_t]}. Ticks, never scaled.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeTotalTardinessContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeTotalTardinessObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        boolean byPriority = MinimizeTotalTardinessObjective.weightBy(objective.params()) == TardinessParams.WeightBy.PRIORITY;
        CandidateIndex index = ctx.index();
        int[] tasks = index.resolveTasks(objective.objective().scope());
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
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
            long maxTardiness = Math.min((long) index.latestEnd(t), index.horizonTicks()) - due;
            if (maxTardiness <= 0) {
                continue;
            }
            long coef = ObjectiveTerms.coef(weight, byPriority ? 1L + index.priority(t) : 1L, "minimize_total_tardiness");
            IntVar tardiness = ctx.newTierVar("tard_" + objective.index() + "_" + t, 0, maxTardiness);
            LinearExpr late = LinearExpr.newBuilder().add(ctx.end(t)).add(-(long) due).build();
            Constraint c = ctx.model().addGreaterOrEqual(tardiness, late);
            if (!index.required(t)) {
                c.onlyEnforceIf(ctx.presence(t));
            }
            constraints++;
            acc.add(tardiness, coef, 0, ScaledMath.mulExact(coef, maxTardiness));
        }
        ctx.count(0, 0, constraints, 0);
        acc.flush(sink, objective.tier(), "minimize_total_tardiness");
    }
}

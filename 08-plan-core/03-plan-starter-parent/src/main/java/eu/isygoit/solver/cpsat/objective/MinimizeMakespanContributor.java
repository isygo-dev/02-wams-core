package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.IntVar;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeMakespanObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_makespan} (design 4.4): one variable {@code M} with {@code M >= end_t} (enforced by
 * the presence of optional tasks); the term is {@code weight * M}. Bounds: {@code M in [lo, hi]} with {@code hi} = largest
 * latest end of the scope (clipped to the horizon) and {@code lo} = largest {@code estStart + minDuration} of its REQUIRED
 * tasks; the sink receives {@code [weight*lo, weight*hi]}. Ticks, never scaled.
 * <p>
 * Stateless, thread-safe (state lives in {@link CpModelContext}).
 */
@Component
public final class MinimizeMakespanContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeMakespanObjective.TYPE;
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
        long lo = 0;
        for (int i = 0; i < tasks.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int t = tasks[i];
            hi = Math.max(hi, Math.min((long) index.latestEnd(t), index.horizonTicks()));
            if (index.required(t)) {
                lo = Math.max(lo, (long) index.estStart(t) + ObjectiveMath.minDuration(index, t));
            }
        }
        lo = Math.min(lo, hi);
        if (hi == 0) {
            return;
        }
        long coef = ObjectiveTerms.coef(weight, 1, "minimize_makespan");
        IntVar makespan = ctx.newTierVar("makespan_" + objective.index(), lo, hi);
        for (int i = 0; i < tasks.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int t = tasks[i];
            Constraint c = ctx.model().addGreaterOrEqual(makespan, ctx.end(t));
            if (!index.required(t)) {
                c.onlyEnforceIf(ctx.presence(t));
            }
        }
        ctx.count(0, 0, tasks.length, 0);
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        acc.add(makespan, coef, ScaledMath.mulExact(coef, lo), ScaledMath.mulExact(coef, hi));
        acc.flush(sink, objective.tier(), "minimize_makespan");
    }
}

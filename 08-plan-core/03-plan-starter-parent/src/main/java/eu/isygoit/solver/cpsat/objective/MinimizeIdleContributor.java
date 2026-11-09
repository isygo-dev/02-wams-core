package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeIdleObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_idle} (design 4.4): for every scoped UNARY resource (capacity 1) that has candidates,
 * variables {@code first_r <= start_t} and {@code last_r >= end_t} enforced by each candidate literal, plus
 * {@code last_r >= first_r}; the term is {@code weight * (last_r - first_r - load_r)} summed over the resources. An unused
 * resource collapses to {@code last = first} (idle 0). Because a unary resource never overlaps its tasks, every term is
 * {@code >= 0}; bounds {@code [0, weight * horizonTicks * resources]}.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeIdleContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeIdleObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        int horizon = index.horizonTicks();
        if (horizon <= 0) {
            return;
        }
        long coef = ObjectiveTerms.coef(weight, 1, "minimize_idle");
        int[][] candidates = ObjectiveMath.candidatesByResource(index);
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        int constraints = 0;
        int[] resources = index.resolveResources(objective.objective().scope());
        for (int i = 0; i < resources.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int r = resources[i];
            if (candidates[r].length == 0 || index.capacity(r) != 1) {
                continue;
            }
            IntVar first = ctx.newTierVar("idlefirst_" + r, 0, horizon);
            IntVar last = ctx.newTierVar("idlelast_" + r, 0, horizon);
            ctx.model().addGreaterOrEqual(last, first);
            constraints++;
            for (int c : candidates[r]) {
                int t = index.candidateTask(c);
                Literal lit = ctx.cand(c);
                ctx.model().addLessOrEqual(first, ctx.start(t)).onlyEnforceIf(lit);
                ctx.model().addGreaterOrEqual(last, ctx.end(t)).onlyEnforceIf(lit);
                constraints += 2;
            }
            acc.term(last, coef);
            acc.term(first, -coef);
            acc.term(ctx.load(r, LoadMetric.TICKS), -coef);
            acc.bounds(0, ScaledMath.mulExact(coef, horizon));
        }
        ctx.count(0, 0, constraints, 0);
        acc.flush(sink, objective.tier(), "minimize_idle");
    }
}

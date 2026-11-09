package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.cpsat.SetupArc;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeSetupObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CP-SAT formulation of {@code minimize_setup} (design 4.4 / 6.6): {@code sum weight * ticks * arc_literal} over the setup
 * arcs registered by the setup circuit module ({@code ctx.setupArcs(r)}) of the scoped resources; arcs with 0 ticks are
 * skipped. A resource with {@code n} candidates has at most {@code n - 1} adjacent pairs, so its bound is
 * {@code min(sum of coefficients, (n - 1) * largest coefficient)}; lower bound 0. Ticks, never scaled. Must run after the
 * structural modules (design 6.1 steps 7 before 10), which the builder guarantees.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeSetupContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeSetupObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        long[] candidateCount = ObjectiveMath.loadUpperBounds(index, LoadMetric.COUNT);
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        int[] resources = index.resolveResources(objective.objective().scope());
        for (int i = 0; i < resources.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int r = resources[i];
            List<SetupArc> arcs = ctx.setupArcs(r);
            if (arcs == null || arcs.isEmpty()) {
                continue;
            }
            long sum = 0;
            long largest = 0;
            for (int a = 0; a < arcs.size(); a++) {
                if (ObjectiveTerms.checkpointDue(a)) {
                    ctx.checkpoint();
                }
                SetupArc arc = arcs.get(a);
                if (arc.ticks() <= 0) {
                    continue;
                }
                long coef = ObjectiveTerms.coef(weight, arc.ticks(), "minimize_setup");
                acc.term(arc.literal(), coef);
                sum = ScaledMath.addExact(sum, coef);
                largest = Math.max(largest, coef);
            }
            long adjacentPairs = Math.max(0L, candidateCount[r] - 1);
            acc.bounds(0, Math.min(sum, ScaledMath.mulExact(adjacentPairs, largest)));
        }
        acc.flush(sink, objective.tier(), "minimize_setup");
    }
}

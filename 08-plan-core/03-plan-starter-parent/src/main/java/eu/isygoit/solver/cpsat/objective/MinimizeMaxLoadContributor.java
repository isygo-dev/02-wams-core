package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.IntVar;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeMaxLoadObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CP-SAT formulation of {@code minimize_max_load} (design 4.4): one variable {@code L in [0, hi]} with {@code L >=
 * load_r} for every scoped resource that has candidates ({@code ctx.load(r, metric)}); the term is {@code weight * L},
 * bounds {@code [0, weight * hi]} where {@code hi} is the largest per-resource upper bound (sum of candidate durations or
 * number of candidates, capped by {@code maxLoadTicks}).
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeMaxLoadContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeMaxLoadObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        LoadMetric metric = ObjectiveMath.metricOf(objective.params());
        boolean[] has = ObjectiveMath.resourcesWithCandidates(index);
        long[] upperBound = ObjectiveMath.loadUpperBounds(index, metric);
        List<Integer> resources = new ArrayList<>();
        long hi = 0;
        for (int r : index.resolveResources(objective.objective().scope())) {
            if (has[r]) {
                resources.add(r);
                hi = Math.max(hi, upperBound[r]);
            }
        }
        if (hi == 0) {
            return;
        }
        long coef = ObjectiveTerms.coef(weight, 1, "minimize_max_load");
        IntVar max = ctx.newTierVar("maxload_" + objective.index(), 0, hi);
        for (int i = 0; i < resources.size(); i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            ctx.model().addLessOrEqual(ctx.load(resources.get(i), metric), max);
        }
        ctx.count(0, 0, resources.size(), 0);
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        acc.add(max, coef, 0, ScaledMath.mulExact(coef, hi));
        acc.flush(sink, objective.tier(), "minimize_max_load");
    }
}

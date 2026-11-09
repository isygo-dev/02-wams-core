package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.IntVar;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.BalanceLoadObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CP-SAT formulation of {@code balance_load} (design 4.4): variables {@code Lmax, Lmin in [0, hi]} with {@code Lmax >=
 * load_r >= Lmin} for every scoped resource that has candidates; the term is {@code weight * (Lmax - Lmin)}, bounds
 * {@code [0, weight * hi]}. Needs at least two such resources (otherwise the term is constantly 0 and nothing is added).
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class BalanceLoadContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return BalanceLoadObjective.TYPE;
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
        if (resources.size() < 2 || hi == 0) {
            return;
        }
        long coef = ObjectiveTerms.coef(weight, 1, "balance_load");
        IntVar max = ctx.newTierVar("balmax_" + objective.index(), 0, hi);
        IntVar min = ctx.newTierVar("balmin_" + objective.index(), 0, hi);
        for (int i = 0; i < resources.size(); i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            ctx.model().addLessOrEqual(ctx.load(resources.get(i), metric), max);
            ctx.model().addGreaterOrEqual(ctx.load(resources.get(i), metric), min);
        }
        ctx.count(0, 0, 2 * resources.size(), 0);
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        acc.term(max, coef);
        acc.term(min, -coef);
        acc.bounds(0, ScaledMath.mulExact(coef, hi));
        acc.flush(sink, objective.tier(), "balance_load");
    }
}

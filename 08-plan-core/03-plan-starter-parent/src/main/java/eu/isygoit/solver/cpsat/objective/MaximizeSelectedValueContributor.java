package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MaximizeSelectedValueObjective;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code maximize_selected_value} (design 4.4): the NEGATED reward {@code - sum weight *
 * effectivePrize_t * presence_t} over the OPTIONAL tasks of the scope. Bounds {@code [-sum coef, 0]}; no constant offset,
 * so the tier value is the negative reward (G11: -190 at tier -1, reported as 190).
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MaximizeSelectedValueContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MaximizeSelectedValueObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        int[] tasks = index.resolveTasks(objective.objective().scope());
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        for (int i = 0; i < tasks.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int t = tasks[i];
            if (index.required(t) || index.prize(t) == 0) {
                continue;
            }
            long coef = ObjectiveTerms.coef(weight, index.prize(t), "maximize_selected_value");
            acc.add(ctx.presence(t), -coef, -coef, 0);
        }
        acc.flush(sink, objective.tier(), "maximize_selected_value");
    }
}

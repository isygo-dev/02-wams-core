package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MaximizeTaskPriorityObjective;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code maximize_task_priority} (design 4.4): the NEGATED reward {@code - sum weight * priority_t *
 * presence_t} over the OPTIONAL tasks of the scope (required tasks are always selected and not part of the term). Bounds
 * {@code [-sum coef, 0]}; no constant offset, so the tier value is the negative reward (the validator negates it back).
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MaximizeTaskPriorityContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MaximizeTaskPriorityObjective.TYPE;
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
            if (index.required(t) || index.priority(t) == 0) {
                continue;
            }
            long coef = ObjectiveTerms.coef(weight, index.priority(t), "maximize_task_priority");
            acc.add(ctx.presence(t), -coef, -coef, 0);
        }
        acc.flush(sink, objective.tier(), "maximize_task_priority");
    }
}

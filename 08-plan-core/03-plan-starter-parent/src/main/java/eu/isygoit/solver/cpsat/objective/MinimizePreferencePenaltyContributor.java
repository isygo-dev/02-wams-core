package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.dto.request.Preference;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizePreferencePenaltyObjective;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_preference_penalty} (design 4.4) over the tasks of the scope:
 * <ul>
 *   <li>RESOURCE preference: {@code weight * penalty * (presence - assigned_r)} (penalty when selected and not on the
 *       resource; an unknown resource is never satisfied), bounds {@code [0, coef]} (a required task contributes a
 *       constant offset {@code coef} and {@code -coef * assigned}, bounds {@code [-coef, 0]});</li>
 *   <li>START preference: {@code weight * penalty * dev}, {@code dev >= |start - preferredTick|} (enforced by presence),
 *       bounds {@code [0, coef * maxDeviation]}.</li>
 * </ul>
 * Penalties are integers, never scaled.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizePreferencePenaltyContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizePreferencePenaltyObjective.TYPE;
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
            for (Preference p : ctx.problem().tasks().get(t).preferences()) {
                if (p.kind() == null || p.penalty() == 0) {
                    continue;
                }
                long coef = ObjectiveTerms.coef(weight, p.penalty(), "minimize_preference_penalty");
                if (p.kind() == Preference.Kind.RESOURCE) {
                    int resource = p.resourceId() == null ? -1 : index.resourceIdx(p.resourceId());
                    ObjectiveTerms.notOnResource(ctx, index, acc, t, resource, coef);
                } else {
                    ObjectiveTerms.startDeviation(ctx, index, acc, t, index.horizon().toTick(p.start()), coef);
                }
            }
        }
        acc.flush(sink, objective.tier(), "minimize_preference_penalty");
    }
}

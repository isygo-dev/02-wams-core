package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizePlanChangesObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_plan_changes} (design 4.4): for every NON-LOCKED {@code initialSolution}
 * assignment of a task in the scope, {@code weight * startDeviationWeight * |start - initialStart|} (auxiliary deviation
 * variable, enforced by presence) plus {@code weight * resourceChangeWeight * (presence - assigned_r)} for each initial
 * resource {@code r} (an unknown resource always counts when the task is selected). Locked assignments are skipped (they
 * cannot deviate). Integers, never scaled.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizePlanChangesContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizePlanChangesObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        long startUnit = MinimizePlanChangesObjective.startWeight(objective.params());
        long resourceUnit = MinimizePlanChangesObjective.resourceWeight(objective.params());
        boolean[] taskMask = new boolean[index.taskCount()];
        for (int t : index.resolveTasks(objective.objective().scope())) {
            taskMask[t] = true;
        }
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        int n = 0;
        for (InitialAssignment a : ctx.problem().initialSolution().assignments()) {
            if (ObjectiveTerms.checkpointDue(n++)) {
                ctx.checkpoint();
            }
            int t = index.taskIdx(a.taskId());
            if (a.locked() || t < 0 || !taskMask[t]) {
                continue;
            }
            if (a.start() != null) {
                long coef = ObjectiveTerms.coef(weight, startUnit, "minimize_plan_changes");
                ObjectiveTerms.startDeviation(ctx, index, acc, t, index.horizon().toTick(a.start()), coef);
            }
            for (String resourceId : a.resourceIds()) {
                long coef = ObjectiveTerms.coef(weight, resourceUnit, "minimize_plan_changes");
                ObjectiveTerms.notOnResource(ctx, index, acc, t, index.resourceIdx(resourceId), coef);
            }
        }
        acc.flush(sink, objective.tier(), "minimize_plan_changes");
    }
}

package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeSlotCostObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CP-SAT formulation of {@code minimize_slot_cost} (design 4.4 / 6.4): for every slot-mode task of the scope the cost of the
 * chosen slot, {@code weight * scale(slot.cost)}, attached to the {@code startChoice} literal of that start value (exactly
 * one is true when the task is present, none when absent). Tasks whose start choice is not materialised
 * ({@code startChoice == null}, more than {@code max-start-choices} anchors) use fallback literals
 * {@code b_v <=> (start == v)} (reified, implied by presence). When several slots of a task share a start tick the lowest
 * slot index prices it (same rule as the evaluator). Bounds per task: {@code [0, largest coefficient]}.
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeSlotCostContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeSlotCostObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        CandidateIndex index = ctx.index();
        int scale = objective.scale();
        int[] tasks = index.resolveTasks(objective.objective().scope());
        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        for (int i = 0; i < tasks.length; i++) {
            if (ObjectiveTerms.checkpointDue(i)) {
                ctx.checkpoint();
            }
            int t = tasks[i];
            int slotBegin = index.taskSlotStart[t];
            int slotEnd = index.taskSlotStart[t + 1];
            if (slotBegin >= slotEnd) {
                continue;
            }
            Map<Integer, Long> costByStart = new LinkedHashMap<>();
            for (int k = slotBegin; k < slotEnd; k++) {
                int slot = index.taskSlots[k];
                long cost = ObjectiveMath.slotCost(ctx.problem(), slot, scale);
                costByStart.putIfAbsent(index.slotStart[slot], ObjectiveTerms.coef(weight, cost, "minimize_slot_cost"));
            }
            long maxCoef = 0;
            Literal[] choice = ctx.startChoice(t);
            if (choice != null) {
                long[] values = ctx.startChoiceValues(t);
                for (int v = 0; v < values.length; v++) {
                    Long coef = costByStart.get((int) values[v]);
                    if (coef == null) {
                        throw new IllegalStateException("start choice value " + values[v] + " of task "
                                + index.taskId(t) + " is not the start of one of its slots");
                    }
                    if (coef > 0 && choice[v] != null) {
                        acc.term(choice[v], coef);
                        maxCoef = Math.max(maxCoef, coef);
                    }
                }
            } else {
                for (Map.Entry<Integer, Long> e : costByStart.entrySet()) {
                    if (e.getValue() <= 0) {
                        continue;
                    }
                    BoolVar at = ctx.model().newBoolVar("slotAt_" + t + "_" + e.getKey());
                    ctx.model().addEquality(ctx.start(t), e.getKey()).onlyEnforceIf(at);
                    ctx.model().addDifferent(ctx.start(t), e.getKey()).onlyEnforceIf(at.not());
                    if (!index.required(t)) {
                        ctx.model().addImplication(at, ctx.presence(t));
                    }
                    ctx.count(1, 0, 3, 0);
                    acc.term(at, e.getValue());
                    maxCoef = Math.max(maxCoef, e.getValue());
                }
            }
            acc.bounds(0, maxCoef);
        }
        acc.flush(sink, objective.tier(), "minimize_slot_cost");
    }
}

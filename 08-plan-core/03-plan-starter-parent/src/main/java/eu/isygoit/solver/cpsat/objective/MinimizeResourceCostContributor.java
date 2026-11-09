package eu.isygoit.solver.cpsat.objective;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.objective.MinimizeResourceCostObjective;
import eu.isygoit.solver.objective.ObjectiveMath;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code minimize_resource_cost} (design 4.4): {@code sum_c cand_c * scale(costPerTick_r) *
 * duration_c + sum_r used_r * scale(costPerUse_r)} over the resources of the scope, every coefficient multiplied by the
 * objective weight and guarded to 2^40. Upper bound: per task, per requirement, the {@code count} largest candidate
 * coefficients (a requirement takes at most {@code count} candidates), plus the costPerUse of the resources that have
 * candidates; lower bound 0. {@code used_r} is {@code ctx.resourceUsed(r, true)} (exact).
 * <p>
 * Stateless, thread-safe.
 */
@Component
public final class MinimizeResourceCostContributor implements CpSatObjectiveContributor {

    @Override
    public String type() {
        return MinimizeResourceCostObjective.TYPE;
    }

    @Override
    public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        long weight = objective.objective().weight();
        if (weight == 0) {
            return;
        }
        PlanningProblem problem = ctx.problem();
        CandidateIndex index = ctx.index();
        int scale = objective.scale();
        int[] resources = index.resolveResources(objective.objective().scope());
        boolean[] inScope = ObjectiveMath.mask(index.resourceCount(), resources);
        boolean[] hasCandidates = ObjectiveMath.resourcesWithCandidates(index);

        long[] perTick = new long[index.resourceCount()];
        for (int r : resources) {
            perTick[r] = ScaledMath.scale(problem.resources().get(r).costPerTick(), scale);
        }

        ObjectiveTerms.Accumulator acc = new ObjectiveTerms.Accumulator();
        for (int t = 0; t < index.taskCount(); t++) {
            if (ObjectiveTerms.checkpointDue(t)) {
                ctx.checkpoint();
            }
            int begin = index.candidateBegin(t);
            int end = index.candidateEnd(t);
            long[] group = new long[Math.max(0, end - begin)];
            long taskMax = 0;
            int c = begin;
            while (c < end) {
                int requirement = index.candReq[c];
                int n = 0;
                int c2 = c;
                while (c2 < end && index.candReq[c2] == requirement) {
                    int r = index.candidateResource(c2);
                    if (inScope[r] && perTick[r] > 0) {
                        long unit = ScaledMath.mulExact(perTick[r], index.candidateDuration(c2));
                        if (unit > 0) {
                            long coef = ObjectiveTerms.coef(weight, unit, "minimize_resource_cost");
                            acc.term(ctx.cand(c2), coef);
                            group[n++] = coef;
                        }
                    }
                    c2++;
                }
                taskMax = ScaledMath.addExact(taskMax, ObjectiveTerms.topSum(group, n, index.reqCount[requirement]));
                c = c2;
            }
            acc.bounds(0, taskMax);
        }

        for (int r : resources) {
            if (!hasCandidates[r]) {
                continue;
            }
            Resource resource = problem.resources().get(r);
            long perUse = ScaledMath.scale(resource.costPerUse(), scale);
            if (perUse > 0) {
                long coef = ObjectiveTerms.coef(weight, perUse, "minimize_resource_cost");
                acc.add(ctx.resourceUsed(r, true), coef, 0, coef);
            }
        }
        acc.flush(sink, objective.tier(), "minimize_resource_cost");
    }
}

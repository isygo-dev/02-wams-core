package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.params.MaxAdjacentTasksParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.MaxAdjacentTasksFamily;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MaxAdjacentTasksContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MaxAdjacentTasksFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MaxAdjacentTasksParams p = (MaxAdjacentTasksParams) constraint.params();
        CandidateIndex index = ctx.index();
        int[] resources = index.resolveResources(constraint.constraint().scope());
        long totalPairs = MaxAdjacentTasksFamily.pairCount(index, constraint.constraint());
        if (!ContribSupport.pairwiseOk(ctx, totalPairs)) {
            return;
        }
        int boolVars = 0;
        int intVars = 0;
        int modelConstraints = 0;
        for (int r : resources) {
            Map<Integer, Integer> candidateByTask = MaxAdjacentTasksFamily.candidatesByTask(index, r);
            List<Integer> tasks = new ArrayList<>(candidateByTask.keySet());
            tasks.sort(Integer::compareTo);
            Map<Integer, List<Literal>> incoming = new HashMap<>();
            Map<Integer, IntVar> lengths = new HashMap<>();
            Map<Integer, BoolVar> hasPredecessor = new HashMap<>();
            for (int t : tasks) {
                lengths.put(t, ctx.model().newIntVar(1, p.max(), ""));
                hasPredecessor.put(t, ctx.model().newBoolVar(""));
                intVars++;
                boolVars += 1;
            }
            for (int from : tasks) {
                int fromCandidate = candidateByTask.get(from);
                int fromDuration = index.candidateDuration(fromCandidate);
                if (fromDuration <= 0) {
                    continue;
                }
                for (int to : tasks) {
                    if (from == to || !MaxAdjacentTasksFamily.canBeAdjacent(index, from, fromDuration, to)) {
                        continue;
                    }
                    int toCandidate = candidateByTask.get(to);
                    BoolVar adjacent = ctx.model().newBoolVar("");
                    Literal fromAssigned = ctx.cand(fromCandidate);
                    Literal toAssigned = ctx.cand(toCandidate);
                    ctx.model().addImplication(adjacent, fromAssigned);
                    ctx.model().addImplication(adjacent, toAssigned);
                    ctx.model().addEquality(ctx.start(to), ctx.end(from)).onlyEnforceIf(adjacent);
                    ctx.model().addDifferent(ctx.start(to), ctx.end(from))
                            .onlyEnforceIf(new Literal[]{fromAssigned, toAssigned, adjacent.not()});
                    ctx.model().addEquality(lengths.get(to), LinearExpr.newBuilder()
                                    .add(lengths.get(from)).add(1).build())
                            .onlyEnforceIf(adjacent);
                    ctx.model().addImplication(adjacent, hasPredecessor.get(to));
                    incoming.computeIfAbsent(to, ignored -> new ArrayList<>()).add(adjacent);
                    boolVars++;
                    modelConstraints += 5;
                }
            }
            for (int t : tasks) {
                int candidate = candidateByTask.get(t);
                Literal assigned = ctx.cand(candidate);
                BoolVar predecessor = hasPredecessor.get(t);
                List<Literal> incomingEdges = incoming.getOrDefault(t, List.of());
                if (!incomingEdges.isEmpty()) {
                    List<Literal> definition = new ArrayList<>(incomingEdges.size() + 1);
                    definition.add(predecessor.not());
                    definition.addAll(incomingEdges);
                    ctx.model().addBoolOr(definition);
                    modelConstraints++;
                } else {
                    ctx.model().addEquality(predecessor, 0);
                    modelConstraints++;
                }
                ctx.model().addEquality(lengths.get(t), 1)
                        .onlyEnforceIf(new Literal[]{assigned, predecessor.not()});
                ctx.model().addLessOrEqual(lengths.get(t), p.max()).onlyEnforceIf(assigned);
                modelConstraints += 2;
            }
        }
        ctx.count(boolVars, intVars, modelConstraints, 0);
    }

}

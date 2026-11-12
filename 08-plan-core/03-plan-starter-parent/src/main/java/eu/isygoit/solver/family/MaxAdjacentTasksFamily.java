package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.MaxAdjacentTasksParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MaxAdjacentTasksFamily implements ConstraintFamily {

    public static final String TYPE = "max_adjacent_tasks";
    public static final String CODE = "MAX_ADJACENT_TASKS_EXCEEDED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MaxAdjacentTasksParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.HARD_ONLY;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.max_adjacent_tasks.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("max");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (params instanceof MaxAdjacentTasksParams p && p.max() < 1) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.max"),
                    "params.max must be >= 1");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof MaxAdjacentTasksParams)) {
            return ModelCost.ZERO;
        }
        long pairs = pairCount(index, constraint);
        long tasks = scopedTaskCount(index, constraint);
        return new ModelCost(pairs + tasks, tasks, pairs * 5 + tasks * 3, 0, 0, pairs);
    }

    public static long pairCount(CandidateIndexView index, Constraint constraint) {
        long count = 0;
        for (int resource : index.resolveResources(constraint.scope())) {
            Map<Integer, Integer> candidates = candidatesByTask(index, resource);
            for (Map.Entry<Integer, Integer> from : candidates.entrySet()) {
                int duration = index.candidateDuration(from.getValue());
                if (duration <= 0) {
                    continue;
                }
                for (Map.Entry<Integer, Integer> to : candidates.entrySet()) {
                    if (!from.getKey().equals(to.getKey())
                            && canBeAdjacent(index, from.getKey(), duration, to.getKey())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static long scopedTaskCount(CandidateIndexView index, Constraint constraint) {
        long count = 0;
        for (int resource : index.resolveResources(constraint.scope())) {
            count += candidatesByTask(index, resource).size();
        }
        return count;
    }

    public static Map<Integer, Integer> candidatesByTask(CandidateIndexView index, int resource) {
        Map<Integer, Integer> result = new java.util.TreeMap<>();
        for (int candidate = 0; candidate < index.candidateCount(); candidate++) {
            if (index.candidateResource(candidate) == resource) {
                result.putIfAbsent(index.candidateTask(candidate), candidate);
            }
        }
        return result;
    }

    public static boolean canBeAdjacent(CandidateIndexView index, int from, int duration, int to) {
        long earliestEnd = (long) index.estStart(from) + duration;
        long latestEnd = (long) index.lstStart(from) + duration;
        return earliestEnd <= index.lstStart(to) && latestEnd >= index.estStart(to);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            MaxAdjacentTasksParams p = (MaxAdjacentTasksParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            for (int r : index.resolveResources(constraint.scope())) {
                int run = 0;
                int previous = -1;
                for (int t : solution.tasksOfResource(r)) {
                    if (previous >= 0 && solution.end(previous) == solution.start(t)) {
                        run++;
                    } else {
                        run = 1;
                    }
                    if (run > p.max()) {
                        sink.violation(index.taskId(t), index.resourceId(r), CODE,
                                "resource " + index.resourceId(r) + " has more than " + p.max()
                                        + " adjacent tasks at " + index.horizon().toInstant(solution.start(t)),
                                1);
                    }
                    previous = t;
                }
            }
        }
    }
}

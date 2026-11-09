package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.params.MinRestParams;
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
import java.util.Set;

/**
 * Constraint family {@code min_rest} (design 4.3, 6.5.8): minimum gap between two tasks on the same UNARY resource.
 * <p>
 * Semantics (shared by the CP-SAT model and the checker): the "inflated interval" of a task on the resource is
 * {@code [start, end + rest)} where {@code rest = minRestTicks} when the task is selected by {@code afterTasks} (null = every
 * task) and 0 otherwise; two tasks conflict when their inflated intervals overlap; an inflated interval of size 0 never
 * conflicts. Scope selects RESOURCES (resource tags). Supports HARD, SOFT, RELAXABLE. Soft unit = ONE conflicting task pair.
 * A scoped resource with capacity other than 1 is rejected with {@code MIN_REST_RESOURCE_NOT_UNARY}. Stateless, thread-safe;
 * ticks.
 */
@Component
public class MinRestFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "min_rest";
    /** Violation code reported by the checker. */
    public static final String CODE = "MIN_REST_VIOLATED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MinRestParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.min_rest.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("minRestTicks", "afterTasks");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        MinRestParams p = (MinRestParams) params;
        if (p == null) {
            return;
        }
        if (p.minRestTicks() < 0) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.minRestTicks"),
                    "params.minRestTicks must be >= 0");
        } else if (p.minRestTicks() == 0) {
            ctx.warning("CONSTRAINT_NO_EFFECT", FamilySupport.path(ctx, constraint, "params.minRestTicks"),
                    "params.minRestTicks is 0: the constraint has no effect");
        }
        ConstraintScope after = p.afterTasks();
        if (after != null && "task".equals(after.kind()) && (after.value() == null || !ctx.hasTask(after.value()))) {
            ctx.error("UNKNOWN_REFERENCE", FamilySupport.path(ctx, constraint, "params.afterTasks.value"),
                    "unknown task '" + after.value() + "' in params.afterTasks");
        }
        for (int r : ctx.index().resolveResources(constraint.scope())) {
            Integer capacity = ctx.problem().resources().get(r).capacity();
            if (capacity != null && capacity != 1) {
                ctx.error("MIN_REST_RESOURCE_NOT_UNARY", FamilySupport.path(ctx, constraint, "scope"),
                        "resource '" + ctx.problem().resources().get(r).id() + "' has capacity " + capacity
                                + ": min_rest needs unary resources");
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        MinRestParams p = (MinRestParams) params;
        if (p == null || p.minRestTicks() <= 0) {
            return ModelCost.ZERO;
        }
        int[] resources = index.resolveResources(constraint.scope());
        int[][] byResource = FamilySupport.candidatesByResource(index);
        boolean soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT
                || constraint.enforcement() == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION;
        long intervals = 0;
        long pairs = 0;
        long cap = index.limits().getMaxPairwise();
        boolean[] after = p.afterTasks() == null ? null
                : FamilySupport.flags(index.taskCount(), index.resolveTasks(p.afterTasks()));
        for (int r : resources) {
            intervals += byResource[r].length;
            if (soft && pairs <= cap) {
                pairs += FamilySupport.conflictPairs(index, byResource[r], after, p.minRestTicks(), cap - pairs).length / 2;
            }
        }
        return new ModelCost(3 * pairs, 0, resources.length + 2 * pairs + pairs, intervals, 0, pairs);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    /** Independent checker: sweeps the tasks of each scoped resource sorted by start. */
    static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            MinRestParams p = (MinRestParams) params;
            int rest = p.minRestTicks();
            if (rest <= 0) {
                return;
            }
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            boolean[] after = p.afterTasks() == null ? null
                    : FamilySupport.flags(index.taskCount(), index.resolveTasks(p.afterTasks()));
            for (int r : index.resolveResources(constraint.scope())) {
                int[] tasks = solution.tasksOfResource(r);
                for (int a = 0; a < tasks.length; a++) {
                    int i = tasks[a];
                    long sizeI = (long) solution.end(i) - solution.start(i) + (after == null || after[i] ? rest : 0);
                    if (sizeI <= 0) {
                        continue;
                    }
                    long boundI = solution.start(i) + sizeI;
                    for (int b = a + 1; b < tasks.length; b++) {
                        int j = tasks[b];
                        if (solution.start(j) >= boundI) {
                            break;
                        }
                        long sizeJ = (long) solution.end(j) - solution.start(j) + (after == null || after[j] ? rest : 0);
                        if (sizeJ <= 0) {
                            continue;
                        }
                        sink.violation(index.taskId(i), index.resourceId(r), CODE,
                                "tasks " + index.taskId(i) + " and " + index.taskId(j) + " on resource "
                                        + index.resourceId(r) + " are closer than the minimum rest of " + rest + " tick(s)",
                                1);
                    }
                }
            }
        }
    }
}

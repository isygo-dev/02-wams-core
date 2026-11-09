package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.GroupingParams;
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
import java.util.TreeMap;

/**
 * Constraint family {@code different_resource} (design 4.3, 6.5.12): the tasks of a group use pairwise different resources
 * (for the role, all roles when null). Groups as in {@link SameResourceFamily}.
 * <p>
 * Soft unit = the number of EXTRA tasks sharing a resource: for each group and resource {@code max(0, tasks - 1)}, one
 * violation per (group, resource) with that measure. Supports HARD, SOFT, RELAXABLE. Stateless, thread-safe.
 */
@Component
public class DifferentResourceFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "different_resource";
    /** Violation code reported by the checker. */
    public static final String CODE = "DIFFERENT_RESOURCE_VIOLATED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return GroupingParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.different_resource.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("groupByTagPrefix", "role");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.GROUPS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        SameResourceFamily.validateGrouping(ctx, constraint, (GroupingParams) params);
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        GroupingParams p = (GroupingParams) params;
        long cands = 0;
        for (int[] group : FamilySupport.groups(index, constraint.scope(), p == null ? null : p.groupByTagPrefix())) {
            for (int t : group) {
                cands += index.candidateEnd(t) - index.candidateBegin(t);
            }
        }
        return new ModelCost(0, cands, cands + 1, 0, 0, 0);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    /** Independent checker: counts, per group and resource, the tasks beyond the first. */
    static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            GroupingParams p = (GroupingParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            for (int[] group : FamilySupport.groups(index, constraint.scope(), p.groupByTagPrefix())) {
                Map<Integer, Integer> perResource = new TreeMap<>();
                Map<Integer, Integer> firstTask = new TreeMap<>();
                for (int t : group) {
                    if (!solution.selected(t)) {
                        continue;
                    }
                    for (int r : FamilySupport.resourcesOf(solution, t, p.role())) {
                        perResource.merge(r, 1, Integer::sum);
                        firstTask.putIfAbsent(r, t);
                    }
                }
                for (Map.Entry<Integer, Integer> e : perResource.entrySet()) {
                    if (e.getValue() > 1) {
                        int r = e.getKey();
                        sink.violation(index.taskId(firstTask.get(r)), index.resourceId(r), CODE,
                                e.getValue() + " tasks of a group share resource " + index.resourceId(r), e.getValue() - 1L);
                    }
                }
            }
        }
    }
}

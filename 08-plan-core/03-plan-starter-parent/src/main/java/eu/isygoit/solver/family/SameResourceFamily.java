package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Constraint family {@code same_resource} (design 4.3, 6.5.11): the tasks of a group use the same resource(s) for a role
 * (all roles when {@code role} is null). Groups are formed by {@code groupByTagPrefix} (one group per matching task tag) or,
 * without prefix, by the scope (one group, {@code kind=tag} typically).
 * <p>
 * Semantics (model and checker): consecutive members {@code (a, b)} of a group (ascending task index) that are BOTH selected
 * must have the same SET of resources for the role. Supports HARD and RELAXABLE (no soft form). One violation per
 * offending consecutive pair (measure 1). Stateless, thread-safe.
 */
@Component
public class SameResourceFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "same_resource";
    /** Violation code reported by the checker. */
    public static final String CODE = "SAME_RESOURCE_VIOLATED";

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
        return FamilySupport.HARD_RELAXABLE;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.same_resource.description";
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
        validateGrouping(ctx, constraint, (GroupingParams) params);
    }

    /** Shared by {@code same_resource} and {@code different_resource}. */
    static void validateGrouping(ValidationContext ctx, Constraint constraint, GroupingParams p) {
        if (p == null) {
            return;
        }
        boolean noPrefix = p.groupByTagPrefix() == null || p.groupByTagPrefix().isBlank();
        if (noPrefix && ConstraintScope.ALL.equals(constraint.scope().kind())) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.groupByTagPrefix"),
                    "params.groupByTagPrefix is required unless the scope selects one group (kind tag)");
        }
        if (p.role() != null && p.role().isBlank()) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.role"),
                    "params.role must not be blank");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        GroupingParams p = (GroupingParams) params;
        long clauses = 0;
        for (int[] group : FamilySupport.groups(index, constraint.scope(), p == null ? null : p.groupByTagPrefix())) {
            for (int t : group) {
                clauses += 2L * (index.candidateEnd(t) - index.candidateBegin(t));
            }
        }
        return new ModelCost(0, 0, clauses, 0, 0, 0);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    /** Independent checker: compares the resource sets of consecutive group members. */
    static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            GroupingParams p = (GroupingParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            for (int[] group : FamilySupport.groups(index, constraint.scope(), p.groupByTagPrefix())) {
                for (int k = 0; k + 1 < group.length; k++) {
                    int a = group[k];
                    int b = group[k + 1];
                    if (!solution.selected(a) || !solution.selected(b)) {
                        continue;
                    }
                    int[] ra = FamilySupport.resourcesOf(solution, a, p.role());
                    int[] rb = FamilySupport.resourcesOf(solution, b, p.role());
                    Arrays.sort(ra);
                    Arrays.sort(rb);
                    if (!Arrays.equals(ra, rb)) {
                        sink.violation(index.taskId(a), ra.length > 0 ? index.resourceId(ra[0]) : null, CODE,
                                "tasks " + index.taskId(a) + " and " + index.taskId(b)
                                        + " must use the same resource but do not", 1);
                    }
                }
            }
        }
    }
}

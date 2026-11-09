package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.ForbiddenPairParams;
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
 * Constraint family {@code forbidden_pair} (design 4.3, 6.5.14). {@code TASK_TASK}: two tasks never on the same resource
 * ({@code SAME_RESOURCE}, the default) or never overlapping in time ({@code TIME_OVERLAP}); {@code RESOURCE_RESOURCE}: two
 * resources never together on one task. The scope is ignored (the pairs name the entities).
 * <p>
 * Soft unit = ONE violated pair for TASK_TASK (even if the tasks share several resources) and ONE (pair, task) for
 * RESOURCE_RESOURCE. Overlap means {@code end_a > start_b and end_b > start_a} (half-open intervals, zero-length tasks
 * included). Supports HARD, SOFT, RELAXABLE. Stateless, thread-safe; ticks.
 */
@Component
public class ForbiddenPairFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "forbidden_pair";
    /** Violation code reported by the checker. */
    public static final String CODE = "FORBIDDEN_PAIR_VIOLATED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return ForbiddenPairParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.forbidden_pair.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("kind", "relation", "pairs");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.NONE;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        ForbiddenPairParams p = (ForbiddenPairParams) params;
        if (p == null) {
            return;
        }
        if (p.kind() == null) {
            ctx.error("MISSING_REQUIRED_FIELD", FamilySupport.path(ctx, constraint, "params.kind"),
                    "params.kind is required (TASK_TASK or RESOURCE_RESOURCE)");
            return;
        }
        if (p.pairs().isEmpty()) {
            ctx.error("MISSING_REQUIRED_FIELD", FamilySupport.path(ctx, constraint, "params.pairs"),
                    "params.pairs must contain at least one pair");
        }
        boolean tasks = p.kind() == ForbiddenPairParams.Kind.TASK_TASK;
        for (int i = 0; i < p.pairs().size(); i++) {
            ForbiddenPairParams.IdPair pair = p.pairs().get(i);
            String where = FamilySupport.path(ctx, constraint, "params.pairs[" + i + "]");
            if (pair == null || pair.a() == null || pair.b() == null) {
                ctx.error("MISSING_REQUIRED_FIELD", where, "both ids of the pair are required");
                continue;
            }
            if (pair.a().equals(pair.b())) {
                ctx.error("PARAMS_INVALID", where, "a pair must name two different " + (tasks ? "tasks" : "resources"));
            }
            for (String id : new String[]{pair.a(), pair.b()}) {
                if (tasks ? !ctx.hasTask(id) : !ctx.hasResource(id)) {
                    ctx.error("UNKNOWN_REFERENCE", where, "unknown " + (tasks ? "task" : "resource") + " '" + id + "'");
                }
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        ForbiddenPairParams p = (ForbiddenPairParams) params;
        if (p == null) {
            return ModelCost.ZERO;
        }
        long pairs = p.pairs().size();
        if (p.kind() == ForbiddenPairParams.Kind.RESOURCE_RESOURCE) {
            return new ModelCost(0, 0, pairs * index.taskCount(), 0, 0, pairs);
        }
        return new ModelCost(3 * pairs, 0, 3 * pairs + pairs * 4, 0, 0, pairs);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    /** Independent checker. */
    static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            ForbiddenPairParams p = (ForbiddenPairParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            for (ForbiddenPairParams.IdPair pair : p.pairs()) {
                if (p.kind() == ForbiddenPairParams.Kind.RESOURCE_RESOURCE) {
                    int r1 = index.resourceIdx(pair.a());
                    int r2 = index.resourceIdx(pair.b());
                    for (int t = 0; t < solution.taskCount(); t++) {
                        if (!solution.selected(t)) {
                            continue;
                        }
                        int[] used = FamilySupport.resourcesOf(solution, t, null);
                        if (FamilySupport.contains(used, r1) && FamilySupport.contains(used, r2)) {
                            sink.violation(index.taskId(t), pair.a(), CODE,
                                    "resources " + pair.a() + " and " + pair.b() + " are together on task " + index.taskId(t),
                                    1);
                        }
                    }
                    continue;
                }
                int a = index.taskIdx(pair.a());
                int b = index.taskIdx(pair.b());
                if (a < 0 || b < 0 || !solution.selected(a) || !solution.selected(b)) {
                    continue;
                }
                if (p.relation() == ForbiddenPairParams.Relation.TIME_OVERLAP) {
                    if (solution.end(a) > solution.start(b) && solution.end(b) > solution.start(a)) {
                        sink.violation(pair.a(), null, CODE, "tasks " + pair.a() + " and " + pair.b() + " overlap in time", 1);
                    }
                } else {
                    int[] ra = FamilySupport.resourcesOf(solution, a, null);
                    int[] rb = FamilySupport.resourcesOf(solution, b, null);
                    for (int r : ra) {
                        if (FamilySupport.contains(rb, r)) {
                            sink.violation(pair.a(), index.resourceId(r), CODE,
                                    "tasks " + pair.a() + " and " + pair.b() + " share resource " + index.resourceId(r), 1);
                            break;
                        }
                    }
                }
            }
        }
    }
}

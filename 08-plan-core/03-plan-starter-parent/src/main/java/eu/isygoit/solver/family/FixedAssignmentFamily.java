package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.FixedAssignmentParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Constraint family {@code fixed_assignment} (design 4.3, 6.5.13): the scoped tasks take a given start and/or are restricted
 * to given resources (bulk, soft or relaxable form of {@code Task.pin}).
 * <p>
 * Semantics: a selected scoped task satisfies the constraint when (a) {@code start} is null or the task starts exactly at
 * that tick and (b) {@code resourceIds} is empty or every resource assigned to the task for {@code role} (all requirements
 * when null) belongs to {@code resourceIds}. Soft unit = ONE task that deviates in any way (measure 1). HARD is applied
 * by {@link #preprocess} (engine neutral pruning of candidates and start domain); scope selects TASKS. Supports HARD, SOFT,
 * RELAXABLE. Stateless, thread-safe; ticks.
 */
@Component
public class FixedAssignmentFamily implements ConstraintFamily {

    /** Family id. */
    public static final String TYPE = "fixed_assignment";
    /** Violation code reported by the checker. */
    public static final String CODE = "FIXED_ASSIGNMENT_VIOLATED";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return FixedAssignmentParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.ALL_ENFORCEMENTS;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.fixed_assignment.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("start", "resourceIds", "role");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        FixedAssignmentParams p = (FixedAssignmentParams) params;
        if (p == null) {
            return;
        }
        if (p.start() == null && p.resourceIds().isEmpty()) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params"),
                    "at least one of params.start and params.resourceIds is required");
        }
        if (p.start() != null) {
            ctx.tick(p.start(), FamilySupport.path(ctx, constraint, "params.start"));
        }
        for (String id : p.resourceIds()) {
            if (!ctx.hasResource(id)) {
                ctx.error("UNKNOWN_REFERENCE", FamilySupport.path(ctx, constraint, "params.resourceIds"),
                        "unknown resource '" + id + "'");
            }
        }
        if (p.role() != null && p.role().isBlank()) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.role"),
                    "params.role must not be blank");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        int[] tasks = index.resolveTasks(constraint.scope());
        long cands = 0;
        for (int t : tasks) {
            cands += index.candidateEnd(t) - index.candidateBegin(t);
        }
        return new ModelCost(tasks.length, 0, tasks.length + cands, 0, 0, 0);
    }

    /**
     * HARD form: intersect the start domain with the fixed start and prune every candidate whose resource is not allowed.
     * Assumes the {@code CandidateIndexBuilder.Mutable} read API listed in the WP4b report (needs from WP2): candidate
     * enumeration per task, candidate resource index and candidate role.
     */
    @Override
    public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
        FixedAssignmentParams p = (FixedAssignmentParams) params;
        int[] tasks = ctx.resolveTasks(constraint.scope());
        int tick = p.start() == null ? -1 : ctx.horizon().toTick(p.start());
        boolean restrictResources = !p.resourceIds().isEmpty();
        var mutable = ctx.candidates();
        for (int t : tasks) {
            if (p.start() != null) {
                ctx.restrictStart(t, tick, tick);
            }
            if (!restrictResources) {
                continue;
            }
            List<Integer> allowed = new ArrayList<>();
            for (String id : p.resourceIds()) {
                allowed.add(mutable.resourceIdx(id));
            }
            for (int c = mutable.candidateBegin(t); c < mutable.candidateEnd(t); c++) {
                boolean roleMatches = p.role() == null || p.role().equals(mutable.candidateRole(c));
                if (roleMatches && !allowed.contains(mutable.candidateResource(c))) {
                    ctx.pruneCandidate(c, "FIXED_ASSIGNMENT", constraint.id());
                }
            }
        }
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    /** Independent checker: one violation per deviating selected task. */
    static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            FixedAssignmentParams p = (FixedAssignmentParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            int tick = p.start() == null ? -1 : index.horizon().toTick(p.start());
            int[] allowed = p.resourceIds().stream().mapToInt(index::resourceIdx).toArray();
            for (int t : index.resolveTasks(constraint.scope())) {
                if (!solution.selected(t)) {
                    continue;
                }
                String reason = null;
                String resourceId = null;
                if (p.start() != null && solution.start(t) != tick) {
                    reason = "starts at tick " + solution.start(t) + " instead of " + tick;
                }
                if (reason == null && allowed.length > 0) {
                    for (int r : FamilySupport.resourcesOf(solution, t, p.role())) {
                        if (!FamilySupport.contains(allowed, r)) {
                            reason = "uses resource " + index.resourceId(r) + " which is not in the fixed set";
                            resourceId = index.resourceId(r);
                            break;
                        }
                    }
                }
                if (reason != null) {
                    sink.violation(index.taskId(t), resourceId, CODE, "task " + index.taskId(t) + " " + reason, 1);
                }
            }
        }
    }
}

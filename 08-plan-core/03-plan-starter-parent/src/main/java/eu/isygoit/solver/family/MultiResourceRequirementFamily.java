package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.params.MultiResourceRequirementParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.TaskRewriter;
import eu.isygoit.solver.spi.ValidationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Constraint family {@code multi_resource_requirement} (design 4.3, 6.5.10): bulk definition of a resource requirement on
 * every scoped task. It is not a model constraint: {@code DomainNormalizer} rewrites the tasks through {@link TaskRewriter}
 * (HARD only) and the structural model of 6.4 does the rest, so there is nothing to check and no CP-SAT formulation.
 * <p>
 * Rewrite rule: starting from {@link Task#effectiveRequirements()}, the requirement of the same role is REPLACED in place,
 * otherwise the new one is appended; when the task declares no requirement at all the implicit requirement
 * ({@link ResourceRequirement#implicit()}) is REPLACED, never kept next to the new one. Scope selects TASKS; tasks outside the
 * scope are returned unchanged (the rewriter evaluates the scope itself, because it runs before any index exists).
 * Stateless, thread-safe.
 */
@Component
public class MultiResourceRequirementFamily implements ConstraintFamily, TaskRewriter {

    /** Family id. */
    public static final String TYPE = "multi_resource_requirement";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return MultiResourceRequirementParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return FamilySupport.HARD_ONLY;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.multi_resource_requirement.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("role", "selector", "count", "amount");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        MultiResourceRequirementParams p = (MultiResourceRequirementParams) params;
        if (p == null) {
            return;
        }
        if (p.count() != null && p.count() < 1) {
            ctx.error("INVALID_REQUIREMENT", FamilySupport.path(ctx, constraint, "params.count"),
                    "params.count must be >= 1");
        }
        if (p.amount() != null && p.amount() < 1) {
            ctx.error("INVALID_REQUIREMENT", FamilySupport.path(ctx, constraint, "params.amount"),
                    "params.amount must be >= 1");
        }
        ResourceSelector s = p.selector();
        if (s != null) {
            for (String id : s.resourceIds()) {
                if (!"ALL".equals(id) && !ctx.hasResource(id)) {
                    ctx.error("UNKNOWN_REFERENCE", FamilySupport.path(ctx, constraint, "params.selector.resourceIds"),
                            "unknown resource '" + id + "'");
                }
            }
            for (String id : s.excludeResourceIds()) {
                if (!ctx.hasResource(id)) {
                    ctx.error("UNKNOWN_REFERENCE", FamilySupport.path(ctx, constraint, "params.selector.excludeResourceIds"),
                            "unknown resource '" + id + "'");
                }
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        return ModelCost.ZERO;
    }

    @Override
    public ConstraintChecker checker() {
        return (ctx, constraint, params, sink) -> {
            // Rewritten into the task requirements: the structural checker verifies them, nothing family specific remains.
        };
    }

    @Override
    public Task rewrite(Task task, Constraint constraint, Object params) {
        MultiResourceRequirementParams p = (MultiResourceRequirementParams) params;
        if (p == null || !FamilySupport.scopeIncludes(constraint.scope(), task)) {
            return task;
        }
        String role = p.role() == null || p.role().isBlank() ? ResourceRequirement.DEFAULT_ROLE : p.role();
        ResourceRequirement replacement = new ResourceRequirement(role, p.selector(), p.count(), p.amount());
        List<ResourceRequirement> result = new ArrayList<>();
        if (task.requirements().isEmpty()) {
            result.add(replacement);
        } else {
            boolean replaced = false;
            for (ResourceRequirement existing : task.requirements()) {
                if (existing.role().equals(role)) {
                    if (!replaced) {
                        result.add(replacement);
                        replaced = true;
                    }
                } else {
                    result.add(existing);
                }
            }
            if (!replaced) {
                result.add(replacement);
            }
        }
        return withRequirements(task, result);
    }

    private static Task withRequirements(Task t, List<ResourceRequirement> requirements) {
        return new Task(t.id(), t.typeId(), t.name(), t.duration(), t.resourceDurations(), t.demand(), t.priority(),
                t.required(), t.prize(), requirements, t.release(), t.deadline(), t.dueDate(), t.startSlots(), t.pin(),
                t.predecessors(), t.setupClass(), t.tags(), t.preferences(), t.attributes());
    }
}

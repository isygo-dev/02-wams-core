package eu.isygoit.solver.timefold.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.params.TaskEligibilityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.timefold.SlotOptions;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Timefold formulation of {@code task_eligibility} (design 4.3): scoped tasks may only use the resources / slots that
 * match the params (resource ids or {@code "ALL"}, excluded ids, resource types, skills (all), slot ids or {@code "ALL"}).
 * <ul>
 *   <li>HARD: every option outside the allowed set is forbidden (idempotent with the neutral preprocess of the family,
 *       which normally already pruned the candidates: a constraint with {@code active == false} is skipped);</li>
 *   <li>SOFT: every option outside the allowed set costs {@code constraint.weight} (violation unit = one assigned
 *       (task, resource, slot) outside the allowed set).</li>
 * </ul>
 * Support: hard + soft (no relaxation). Stateless Spring bean, thread-safe; runs on the building thread. Scope: TASKS.
 */
@Component
public class TaskEligibilityTimefoldContributor implements TimefoldConstraintContributor {

    /** The family id. */
    public static final String TYPE = "task_eligibility";

    private static final String ALL = "ALL";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return new EnforcementSupport(true, true, false);
    }

    @Override
    public void contribute(TimefoldBuildContext ctx, CompiledConstraint compiled) {
        if (!compiled.active() || !(compiled.params() instanceof TaskEligibilityParams params)) {
            return;
        }
        Constraint constraint = compiled.constraint();
        boolean soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT;
        boolean[] taskMask = ctx.taskMask(constraint.scope());
        SlotOptions options = ctx.options();
        boolean allResources = params.resourceIds().isEmpty() || containsAll(params.resourceIds());
        boolean allSlots = params.slotIds().isEmpty() || containsAll(params.slotIds());
        Set<String> resourceIds = new HashSet<>(params.resourceIds());
        Set<String> excluded = new HashSet<>(params.excludeResourceIds());
        Set<String> types = new HashSet<>(params.resourceTypeIds());
        Set<String> slotIds = new HashSet<>(params.slotIds());
        boolean[] resourceAllowed = new boolean[ctx.index().resourceCount()];
        for (int r = 0; r < resourceAllowed.length; r++) {
            resourceAllowed[r] = resourceAllowed(ctx.resource(r), allResources, resourceIds, excluded, types, params.skills());
        }
        for (int t = 0; t < options.taskCount(); t++) {
            if (taskMask != null && !taskMask[t]) {
                continue;
            }
            for (int o = options.taskOptionStart(t); o < options.taskOptionEnd(t); o++) {
                boolean allowed = resourceAllowed[options.resource(o)]
                        && (allSlots || slotIds.contains(ctx.index().slotId(options.slot(o))));
                if (allowed) {
                    continue;
                }
                if (soft) {
                    ctx.addPenalty(o, constraint.weight());
                } else {
                    ctx.forbid(o, constraint.id());
                }
            }
        }
    }

    private static boolean containsAll(List<String> ids) {
        for (String id : ids) {
            if (ALL.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean resourceAllowed(Resource resource, boolean allResources, Set<String> ids, Set<String> excluded,
                                           Set<String> types, List<String> skills) {
        if (!allResources && !ids.contains(resource.id())) {
            return false;
        }
        if (excluded.contains(resource.id())) {
            return false;
        }
        if (!types.isEmpty() && !types.contains(resource.typeId())) {
            return false;
        }
        return skills.isEmpty() || resource.skills().containsAll(skills);
    }
}

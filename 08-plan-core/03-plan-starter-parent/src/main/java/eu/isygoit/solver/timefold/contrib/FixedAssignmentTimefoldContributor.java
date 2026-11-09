package eu.isygoit.solver.timefold.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.FixedAssignmentParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.timefold.SlotOptions;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Timefold formulation of {@code fixed_assignment} (design 4.3): scoped tasks take the given start and/or resources, as
 * NEUTRAL pruning of options: an option whose start differs from {@code params.start} (floored to the tick grid) is
 * forbidden, and, when {@code params.resourceIds} is not empty and the task's requirement role matches
 * {@code params.role} (or the role is {@code null}), an option on another resource is forbidden. Idempotent with the
 * family preprocess (a constraint with {@code active == false} is skipped). HARD only. Stateless Spring bean,
 * thread-safe; runs on the building thread. Scope: TASKS.
 */
@Component
public class FixedAssignmentTimefoldContributor implements TimefoldConstraintContributor {

    /** The family id. */
    public static final String TYPE = "fixed_assignment";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void contribute(TimefoldBuildContext ctx, CompiledConstraint compiled) {
        if (!compiled.active() || !(compiled.params() instanceof FixedAssignmentParams params)) {
            return;
        }
        Constraint constraint = compiled.constraint();
        CandidateIndex index = ctx.index();
        boolean hasStart = params.start() != null;
        long start = hasStart ? index.horizon().floorTick(params.start()) : 0L;
        Set<String> resourceIds = new HashSet<>(params.resourceIds());
        boolean[] taskMask = ctx.taskMask(constraint.scope());
        SlotOptions options = ctx.options();
        for (int t = 0; t < options.taskCount(); t++) {
            if (taskMask != null && !taskMask[t]) {
                continue;
            }
            boolean resourcesApply = !resourceIds.isEmpty() && roleMatches(index, t, params.role());
            for (int o = options.taskOptionStart(t); o < options.taskOptionEnd(t); o++) {
                boolean wrongStart = hasStart && options.start(o) != start;
                boolean wrongResource = resourcesApply && !resourceIds.contains(index.resourceId(options.resource(o)));
                if (wrongStart || wrongResource) {
                    ctx.forbid(o, constraint.id());
                }
            }
        }
    }

    /** The Timefold subset has exactly one requirement per task, so the role is that of requirement 0. */
    private static boolean roleMatches(CandidateIndex index, int task, String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        int requirement = index.taskReqStart[task];
        return role.equals(index.roles[index.reqRole[requirement]]);
    }
}

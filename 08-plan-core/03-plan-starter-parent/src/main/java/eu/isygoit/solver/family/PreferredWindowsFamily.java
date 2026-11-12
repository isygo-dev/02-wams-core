package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.params.PreferredWindowsParams;
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

@Component
public class PreferredWindowsFamily implements ConstraintFamily {

    public static final String TYPE = "preferred_windows";
    public static final String CODE = "PREFERRED_WINDOW_NOT_MET";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return PreferredWindowsParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return Set.of(IEnumEnforcement.Types.SOFT);
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.preferred_windows.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("windows", "resourceId");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof PreferredWindowsParams p)) {
            return;
        }
        if (p.windows().isEmpty()) {
            ctx.error("PARAMS_INVALID", FamilySupport.path(ctx, constraint, "params.windows"),
                    "params.windows must contain at least one window");
        }
        if (p.resourceId() != null && !p.resourceId().isBlank() && !ctx.hasResource(p.resourceId())) {
            ctx.error("RESOURCE_NOT_FOUND", FamilySupport.path(ctx, constraint, "params.resourceId"),
                    "params.resourceId does not identify a resource");
        }
        for (int i = 0; i < p.windows().size(); i++) {
            PreferredWindowsParams.Window window = p.windows().get(i);
            String path = FamilySupport.path(ctx, constraint, "params.windows[" + i + "]");
            if (window == null || window.start() == null || window.end() == null) {
                ctx.error("PARAMS_INVALID", path, "each preferred window requires start and end");
                continue;
            }
            ctx.tick(window.start(), path + ".start");
            ctx.tick(window.end(), path + ".end");
            if (!window.start().isBefore(window.end())) {
                ctx.error("PARAMS_INVALID", path, "window start must be before end");
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof PreferredWindowsParams p)) {
            return ModelCost.ZERO;
        }
        long tasks = index.resolveTasks(constraint.scope()).length;
        long windows = p.windows().size();
        return new ModelCost(tasks * windows, tasks, tasks * (2 * windows + 2), 0, 0, 0);
    }

    @Override
    public ConstraintChecker checker() {
        return new Checker();
    }

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            PreferredWindowsParams p = (PreferredWindowsParams) params;
            CandidateIndexView index = ctx.index();
            SolutionView solution = ctx.solution();
            int resource = p.resourceId() == null || p.resourceId().isBlank()
                    ? -1 : index.resourceIdx(p.resourceId());
            for (int t : index.resolveTasks(constraint.scope())) {
                if (!solution.selected(t) || resource >= 0 && !FamilySupport.contains(
                        FamilySupport.resourcesOf(solution, t, null), resource)) {
                    continue;
                }
                boolean inside = p.windows().stream().anyMatch(window -> {
                    int start = index.horizon().floorTick(window.start());
                    int end = index.horizon().floorTick(window.end());
                    return solution.start(t) >= start && solution.end(t) <= end;
                });
                if (!inside) {
                    sink.violation(index.taskId(t), resource < 0 ? null : p.resourceId(), CODE,
                            "task " + index.taskId(t) + " is outside all preferred time windows", 1);
                }
            }
        }
    }
}

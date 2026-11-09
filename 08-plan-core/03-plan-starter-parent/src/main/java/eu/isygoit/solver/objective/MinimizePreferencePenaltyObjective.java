package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Preference;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.util.ScaledMath;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code minimize_preference_penalty} (design 4.4): {@code sum} of the {@code Task.preferences} penalties violated by the
 * SELECTED tasks of the scope. {@code RESOURCE}: {@code penalty} when the task is NOT assigned to {@code resourceId}
 * (an unknown resource is never satisfied). {@code START}: {@code penalty * |start - preferredStart|} in ticks.
 * Penalties are integers, never scaled.
 * <p>
 * Stateless, thread-safe. CP-SAT formulation: {@code MinimizePreferencePenaltyContributor}.
 */
@Component
public class MinimizePreferencePenaltyObjective extends AbstractObjectiveFamily {

    /** Family id. */
    public static final String TYPE = "minimize_preference_penalty";

    public MinimizePreferencePenaltyObjective() {
        super(TYPE, Void.class, false, List.of(), ScopeKind.TASKS);
    }

    @Override
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
        boolean any = false;
        List<Task> tasks = ctx.problem().tasks();
        for (int i = 0; i < tasks.size(); i++) {
            List<Preference> prefs = tasks.get(i).preferences();
            for (int j = 0; j < prefs.size(); j++) {
                any = true;
                Preference p = prefs.get(j);
                String path = "tasks[" + i + "].preferences[" + j + "]";
                if (p.kind() == null) {
                    ctx.error(OBJECTIVE_PARAMS_INVALID, path + ".kind", "preference kind is missing");
                } else if (p.kind() == Preference.Kind.RESOURCE) {
                    if (p.resourceId() == null || !ctx.hasResource(p.resourceId())) {
                        ctx.error(UNKNOWN_REFERENCE, path + ".resourceId", "unknown resource '" + p.resourceId() + "'");
                    }
                } else {
                    ctx.tick(p.start(), path + ".start");
                }
                if (p.penalty() < 0) {
                    ctx.error(OBJECTIVE_PARAMS_INVALID, path + ".penalty", "penalty must be >= 0");
                }
            }
        }
        if (!any) {
            noEffect(ctx, objective, "no task declares preferences");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
        return new ModelCost(0, index.taskCount(), 2L * index.taskCount(), 0, 0, 0);
    }

    @Override
    protected long evaluate(CheckContext ctx, Objective objective, Object params) {
        SolutionView solution = ctx.solution();
        CandidateIndexView index = ctx.index();
        long total = 0;
        for (int t : index.resolveTasks(objective.scope())) {
            if (!solution.selected(t)) {
                continue;
            }
            for (Preference p : ctx.problem().tasks().get(t).preferences()) {
                if (p.kind() == Preference.Kind.RESOURCE) {
                    int wanted = p.resourceId() == null ? -1 : index.resourceIdx(p.resourceId());
                    boolean assigned = false;
                    for (int k = 0; k < solution.resourceCount(t); k++) {
                        assigned |= solution.resourceAt(t, k) == wanted;
                    }
                    if (!assigned) {
                        total = ScaledMath.addExact(total, p.penalty());
                    }
                } else if (p.kind() == Preference.Kind.START) {
                    int preferred = index.horizon().toTick(p.start());
                    long deviation = Math.abs((long) solution.start(t) - preferred);
                    total = ScaledMath.addExact(total, ScaledMath.mulExact(p.penalty(), deviation));
                }
            }
        }
        return ScaledMath.guard(total, "minimize_preference_penalty");
    }
}

package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.TaskEligibilityParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.TaskEligibilityFamily;
import eu.isygoit.solver.spi.CandidateIndexView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CP-SAT formulation of {@code task_eligibility} (design 6.5.1) for SOFT, RELAXABLE and the explain pass; the plain
 * HARD solve is a no-op because {@link TaskEligibilityFamily#preprocess} already pruned the index.
 * <p>
 * Per scoped task: every candidate on a not-allowed resource is forbidden by {@code not cand(c)} enforced by
 * {@code enforcer.instance(task, resource)} (SOFT unit = one forbidden (task, resource) used). When slot ids restrict the
 * start, one instance per task forbids the start values outside the allowed slot starts (through the slot start-choice
 * literals when the task is in slot mode, else through "start equals an allowed value" literals).
 * <p>
 * Stateless and thread-safe (state lives in the {@link CpModelContext}); units are ticks.
 */
@Component
public class TaskEligibilityContributor implements CpSatConstraintContributor {

    private static final int CHECKPOINT_INTERVAL = 1024;

    @Override
    public String type() {
        return TaskEligibilityFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        if (enforcer.mode() == Enforcer.Mode.HARD && !ctx.explainMode()) {
            return; // handled by preprocess (candidate pruning + start-domain restriction)
        }
        TaskEligibilityParams p = (TaskEligibilityParams) constraint.params();
        CandidateIndexView idx = ctx.index();
        CpModel model = ctx.model();
        boolean[] allowed = TaskEligibilityFamily.allowedResources(ctx.problem(), p);
        int[] starts = TaskEligibilityFamily.allowedSlotStarts(ctx.problem(), p);
        int constraints = 0;
        int boolVars = 0;
        int iterations = 0;
        for (int t : idx.resolveTasks(constraint.constraint().scope())) {
            for (int c = idx.candidateBegin(t); c < idx.candidateEnd(t); c++) {
                if (++iterations % CHECKPOINT_INTERVAL == 0) {
                    ctx.checkpoint();
                }
                int r = idx.candidateResource(c);
                if (allowed[r]) {
                    continue;
                }
                Literal instance = enforcer.instance(t, r);
                enforce(model.addBoolOr(new Literal[]{ctx.cand(c).not()}), enforcer.conditions(instance));
                constraints++;
            }
            if (starts != null) {
                int[] added = restrictStart(ctx, enforcer, t, starts);
                constraints += added[0];
                boolVars += added[1];
            }
        }
        ctx.count(boolVars, 0, constraints, 0);
    }

    /** @return {constraints added, bool vars added} */
    private static int[] restrictStart(CpModelContext ctx, Enforcer enforcer, int t, int[] starts) {
        CpModel model = ctx.model();
        Literal[] choice = ctx.startChoice(t);
        if (choice != null) {
            long[] values = ctx.startChoiceValues(t);
            List<Literal> forbidden = new ArrayList<>();
            for (int k = 0; k < choice.length; k++) {
                if (Arrays.binarySearch(starts, (int) values[k]) < 0) {
                    forbidden.add(choice[k]);
                }
            }
            if (forbidden.isEmpty()) {
                return new int[]{0, 0};
            }
            Literal[] conds = enforcer.conditions(enforcer.instance(t, -1), optionalPresence(ctx, t));
            for (Literal lit : forbidden) {
                enforce(model.addBoolOr(new Literal[]{lit.not()}), conds);
            }
            return new int[]{forbidden.size(), 0};
        }
        Literal[] conds = enforcer.conditions(enforcer.instance(t, -1), optionalPresence(ctx, t));
        List<Literal> equalities = new ArrayList<>();
        for (int value : starts) {
            BoolVar eq = model.newBoolVar("");
            model.addEquality(ctx.start(t), value).onlyEnforceIf(eq); // definitional: eq => start == value
            equalities.add(eq);
        }
        enforce(model.addBoolOr(equalities.toArray(new Literal[0])), conds);
        return new int[]{starts.length + 1, starts.length};
    }

    /** @return the presence literal of an optional task, {@code null} (no condition) for a required task */
    private static Literal optionalPresence(CpModelContext ctx, int t) {
        Literal p = ctx.presence(t);
        return p == ctx.trueLit() ? null : p;
    }

    private static void enforce(com.google.ortools.sat.Constraint constraint, Literal[] conditions) {
        if (conditions != null && conditions.length > 0) {
            constraint.onlyEnforceIf(conditions);
        }
    }
}

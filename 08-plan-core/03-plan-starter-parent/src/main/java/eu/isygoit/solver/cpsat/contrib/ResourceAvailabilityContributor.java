package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.ResourceAvailabilityParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.ResourceAvailabilityFamily;
import eu.isygoit.solver.spi.CandidateIndexView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CP-SAT formulation of {@code resource_availability} (design 6.5.6) for SOFT, RELAXABLE and the explain pass; the plain
 * HARD solve is a no-op because {@link ResourceAvailabilityFamily#preprocess} already removed the forbidden starts of every
 * candidate.
 * <p>
 * Per candidate of a scoped resource (duration {@code d > 0}) and per FORBIDDEN START run {@code [a, b]} reachable inside
 * the start domain (the starts whose interval overlaps blocked time): {@code not cand or before or after} with
 * {@code before => start <= a-1} and {@code after => start >= b+1}. All clauses of one candidate are enforced by the SAME
 * {@code enforcer.instance(task, resource)}, so the SOFT unit (overtime) is one assigned (task, resource) pair that overlaps
 * blocked time, exactly what the checker counts. No domain object is used (only boolean / linear constraints), so the
 * formulation is native-free. Stateless and thread-safe; units are ticks.
 */
@Component
public class ResourceAvailabilityContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return ResourceAvailabilityFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        if (enforcer.mode() == Enforcer.Mode.HARD && !ctx.explainMode()) {
            return; // handled by preprocess (per-candidate start runs)
        }
        ResourceAvailabilityParams p = (ResourceAvailabilityParams) constraint.params();
        CandidateIndexView idx = ctx.index();
        CpModel model = ctx.model();
        int[] blocked = ResourceAvailabilityFamily.blockedRuns(idx.horizon(), p);
        if (blocked.length == 0) {
            return;
        }
        boolean[] inScope = ResourceAvailabilityFamily.scopeMask(idx, constraint.constraint());
        Map<Integer, int[]> forbiddenByDuration = new HashMap<>();
        int boolVars = 0;
        int constraints = 0;
        int iterations = 0;
        for (int t = 0; t < idx.taskCount(); t++) {
            int est = idx.estStart(t);
            int lst = idx.lstStart(t);
            for (int c = idx.candidateBegin(t); c < idx.candidateEnd(t); c++) {
                if (++iterations % 1024 == 0) {
                    ctx.checkpoint();
                }
                int r = idx.candidateResource(c);
                int d = idx.candidateDuration(c);
                if (!inScope[r] || d <= 0) {
                    continue; // a zero-duration task occupies nothing
                }
                int[] forbidden = forbiddenByDuration.computeIfAbsent(d, k -> ResourceAvailabilityFamily.forbiddenStartRuns(blocked, k));
                List<int[]> reachable = new ArrayList<>();
                for (int k = 0; k < forbidden.length; k += 2) {
                    if (forbidden[k + 1] >= est && forbidden[k] <= lst) {
                        reachable.add(new int[]{forbidden[k], forbidden[k + 1]});
                    }
                }
                if (reachable.isEmpty()) {
                    continue; // the domains already keep the task out of blocked time
                }
                Literal[] conditions = enforcer.conditions(enforcer.instance(t, r)); // ONE instance per candidate
                Literal candidate = ctx.cand(c);
                for (int[] run : reachable) {
                    List<Literal> clause = new ArrayList<>();
                    clause.add(candidate.not());
                    if (run[0] - 1 >= est) {
                        BoolVar before = model.newBoolVar("");
                        model.addLessOrEqual(ctx.start(t), run[0] - 1L).onlyEnforceIf(before); // definitional
                        clause.add(before);
                        boolVars++;
                        constraints++;
                    }
                    if (run[1] + 1 <= lst) {
                        BoolVar after = model.newBoolVar("");
                        model.addGreaterOrEqual(ctx.start(t), run[1] + 1L).onlyEnforceIf(after); // definitional
                        clause.add(after);
                        boolVars++;
                        constraints++;
                    }
                    com.google.ortools.sat.Constraint rule = model.addBoolOr(clause.toArray(new Literal[0]));
                    if (conditions.length > 0) {
                        rule.onlyEnforceIf(conditions);
                    }
                    constraints++;
                }
            }
        }
        ctx.count(boolVars, 0, constraints, 0);
    }
}

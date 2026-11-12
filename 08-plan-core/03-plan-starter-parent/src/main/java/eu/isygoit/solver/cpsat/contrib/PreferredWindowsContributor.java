package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.PreferredWindowsParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.PreferredWindowsFamily;
import eu.isygoit.solver.spi.CandidateIndexView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PreferredWindowsContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return PreferredWindowsFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return new EnforcementSupport(false, true, false);
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        PreferredWindowsParams p = (PreferredWindowsParams) constraint.params();
        CandidateIndexView index = ctx.index();
        int resource = p.resourceId() == null || p.resourceId().isBlank()
                ? -1 : index.resourceIdx(p.resourceId());
        int boolVars = 0;
        int constraints = 0;
        for (int t : index.resolveTasks(constraint.constraint().scope())) {
            Literal active = resource < 0 ? ctx.presence(t) : candidateFor(ctx, index, t, resource);
            if (active == null) {
                continue;
            }
            Literal satisfied = enforcer.instance(t, resource);
            List<Literal> inside = new ArrayList<>();
            for (PreferredWindowsParams.Window window : p.windows()) {
                int lo = index.horizon().floorTick(window.start());
                int hi = index.horizon().floorTick(window.end());
                BoolVar fits = ctx.model().newBoolVar("");
                inside.add(fits);
                ctx.model().addImplication(fits, active);
                ctx.model().addGreaterOrEqual(ctx.start(t), lo).onlyEnforceIf(fits);
                ctx.model().addLessOrEqual(ctx.end(t), hi).onlyEnforceIf(fits);
                boolVars++;
                constraints += 3;
            }
            ctx.model().addBoolOr(new Literal[]{active, satisfied});
            List<Literal> satisfaction = new ArrayList<>(inside.size() + 2);
            satisfaction.add(satisfied.not());
            satisfaction.add(active.not());
            satisfaction.addAll(inside);
            ctx.model().addBoolOr(satisfaction);
            constraints += 2;
        }
        ctx.count(boolVars, 0, constraints, 0);
    }

    private static Literal candidateFor(CpModelContext ctx, CandidateIndexView index, int task, int resource) {
        for (int c = index.candidateBegin(task); c < index.candidateEnd(task); c++) {
            if (index.candidateResource(c) == resource) {
                return ctx.cand(c);
            }
        }
        return null;
    }
}

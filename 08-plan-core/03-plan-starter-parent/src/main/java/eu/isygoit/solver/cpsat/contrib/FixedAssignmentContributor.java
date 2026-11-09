package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.FixedAssignmentParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.family.FixedAssignmentFamily;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CP-SAT formulation of {@code fixed_assignment} (design 6.5.13) for SOFT, RELAXABLE and the explain pass. In the HARD solve
 * pass the family's neutral {@code preprocess} already pruned the candidates and the start domain, so {@code apply} is a
 * no-op there. Per scoped task ONE instance literal (soft unit = one deviating task, the checker's unit) enforces, together
 * with the presence of the task, the fixed start and the exclusion of every candidate outside the allowed resource set (for
 * the role, all requirements when null). Stateless, thread-safe; ticks.
 */
@Component
public class FixedAssignmentContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return FixedAssignmentFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        if (enforcer.mode() == Enforcer.Mode.HARD && !ctx.explainMode()) {
            return;
        }
        FixedAssignmentParams p = (FixedAssignmentParams) constraint.params();
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        int tick = p.start() == null ? -1 : index.horizon().toTick(p.start());
        int[] allowed = p.resourceIds().stream().mapToInt(index::resourceIdx).toArray();
        for (int t : index.resolveTasks(constraint.constraint().scope())) {
            ctx.checkpoint();
            List<Literal> forbidden = new ArrayList<>();
            if (allowed.length > 0) {
                for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                    boolean roleMatches = p.role() == null || p.role().equals(ContribSupport.roleOf(index, c));
                    if (roleMatches && !FamilySupport.contains(allowed, index.candidateResource(c))) {
                        forbidden.add(ctx.cand(c));
                    }
                }
            }
            if (p.start() == null && forbidden.isEmpty()) {
                continue;
            }
            Literal[] condition = enforcer.conditions(enforcer.instance(t, -1), ctx.presence(t));
            if (p.start() != null) {
                ContribSupport.enforce(model.addEquality(ctx.start(t), tick), condition);
            }
            for (Literal literal : forbidden) {
                ContribSupport.enforce(model.addEquality(literal, 0), condition);
            }
            ContribSupport.count(ctx, 0, 0, 1L + forbidden.size(), 0);
        }
    }
}

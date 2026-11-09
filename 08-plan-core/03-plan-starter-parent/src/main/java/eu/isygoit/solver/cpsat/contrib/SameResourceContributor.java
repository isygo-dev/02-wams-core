package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.GroupingParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.family.SameResourceFamily;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * CP-SAT formulation of {@code same_resource} (design 6.5.11): for every pair of consecutive group members {@code (a, b)} and
 * every resource {@code r}, "a on r implies b on r" and "b on r implies a on r" as clauses guarded by the presence of both
 * tasks, so that both selected tasks end up with the same resource set for the role. One instance literal per consecutive
 * pair (the checker's unit). HARD and RELAXABLE only (no soft form), through the {@link Enforcer}. Stateless, thread-safe.
 */
@Component
public class SameResourceContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return SameResourceFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return new EnforcementSupport(true, false, true);
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        GroupingParams p = (GroupingParams) constraint.params();
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        for (int[] group : FamilySupport.groups(index, constraint.constraint().scope(), p.groupByTagPrefix())) {
            for (int k = 0; k + 1 < group.length; k++) {
                ctx.checkpoint();
                int a = group[k];
                int b = group[k + 1];
                Map<Integer, List<Literal>> onA = ContribSupport.literalsByResource(ctx, index, a, p.role());
                Map<Integer, List<Literal>> onB = ContribSupport.literalsByResource(ctx, index, b, p.role());
                TreeSet<Integer> resources = new TreeSet<>(onA.keySet());
                resources.addAll(onB.keySet());
                List<Literal[]> clauses = new ArrayList<>();
                Literal notBoth1 = ctx.presence(a).not();
                Literal notBoth2 = ctx.presence(b).not();
                for (int r : resources) {
                    List<Literal> la = onA.getOrDefault(r, List.of());
                    List<Literal> lb = onB.getOrDefault(r, List.of());
                    for (Literal ca : la) {
                        clauses.add(clause(notBoth1, notBoth2, ca.not(), lb));
                    }
                    for (Literal cb : lb) {
                        clauses.add(clause(notBoth1, notBoth2, cb.not(), la));
                    }
                }
                if (clauses.isEmpty()) {
                    continue;
                }
                Literal instance = enforcer.instance(a, -1);
                for (Literal[] clause : clauses) {
                    ContribSupport.enforce(model.addBoolOr(clause), instance);
                }
                ContribSupport.count(ctx, 0, 0, clauses.size(), 0);
            }
        }
    }

    private static Literal[] clause(Literal notA, Literal notB, Literal self, List<Literal> others) {
        Literal[] out = new Literal[3 + others.size()];
        out[0] = notA;
        out[1] = notB;
        out[2] = self;
        for (int i = 0; i < others.size(); i++) {
            out[3 + i] = others.get(i);
        }
        return out;
    }
}

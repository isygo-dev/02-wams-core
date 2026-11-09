package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.params.GroupingParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.DifferentResourceFamily;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CP-SAT formulation of {@code different_resource} (design 6.5.12): per group and resource the number of group tasks assigned
 * to it is at most one. HARD / RELAXABLE enforce {@code sum <= 1} with the instance literal of the (resource); SOFT bounds
 * {@code sum - excess <= 1} and penalises {@code excess} (unit = extra tasks sharing the resource, the checker's measure).
 * Stateless, thread-safe.
 */
@Component
public class DifferentResourceContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return DifferentResourceFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        GroupingParams p = (GroupingParams) constraint.params();
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        for (int[] group : FamilySupport.groups(index, constraint.constraint().scope(), p.groupByTagPrefix())) {
            ctx.checkpoint();
            Map<Integer, List<Literal>> perResource = new TreeMap<>();
            for (int t : group) {
                for (Map.Entry<Integer, List<Literal>> e : ContribSupport.literalsByResource(ctx, index, t, p.role()).entrySet()) {
                    perResource.computeIfAbsent(e.getKey(), x -> new ArrayList<>()).addAll(e.getValue());
                }
            }
            for (Map.Entry<Integer, List<Literal>> e : perResource.entrySet()) {
                List<Literal> literals = e.getValue();
                if (literals.size() < 2) {
                    continue;
                }
                int r = e.getKey();
                if (enforcer.mode() == Enforcer.Mode.SOFT) {
                    long maxExcess = literals.size() - 1L;
                    IntVar excess = model.newIntVar(0, maxExcess, "");
                    if (enforcer.penalizeMeasure(excess.build(), maxExcess, -1)) {
                        LinearExprBuilder b = LinearExpr.newBuilder();
                        for (Literal l : literals) {
                            b.add(l);
                        }
                        b.addTerm(excess, -1);
                        model.addLessOrEqual(b.build(), 1);
                        ContribSupport.count(ctx, 0, 1, 1, 0);
                        continue;
                    }
                }
                LinearExprBuilder sum = LinearExpr.newBuilder();
                for (Literal l : literals) {
                    sum.add(l);
                }
                ContribSupport.enforce(model.addLessOrEqual(sum.build(), 1), enforcer.instance(-1, r));
                ContribSupport.count(ctx, 0, 0, 1, 0);
            }
        }
    }
}

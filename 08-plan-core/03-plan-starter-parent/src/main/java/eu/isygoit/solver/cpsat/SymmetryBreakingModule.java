package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Structural module (order 800, design 6.7) that breaks the symmetry of interchangeable tasks and resources in
 * O(T + C):
 * <ul>
 *   <li>tasks with the same {@code taskSymmetryClass} (ascending index): {@code start(i) <= start(j)}; for optional tasks also
 *   {@code presence(i) >= presence(j)};</li>
 *   <li>resources with the same {@code resourceSymmetryClass} (ascending index): {@code used(r_k) >= used(r_k+1)} with the
 *   exact used literals.</li>
 * </ul>
 * The classes already exclude every entity referenced by id anywhere (constraint scopes, pins, preferences, locks, tag
 * groups: index step 7). The module does nothing in EXPLAIN builds, when {@code app.plan.cpsat.symmetry-breaking=false},
 * or when the problem carries locked assignments.
 * <p>
 * Thread-safety: stateless Spring singleton; building thread only. Units: ticks.
 */
@Component
@Order(800)
public class SymmetryBreakingModule implements CpSatStructuralModule {

    /** Module order (after the setup circuit module of order 700). */
    public static final int ORDER = 800;

    private final CpSatProperties properties;

    /**
     * @param properties the CP-SAT properties ({@code symmetry-breaking} kill switch)
     */
    public SymmetryBreakingModule(CpSatProperties properties) {
        this.properties = properties == null ? new CpSatProperties() : properties;
    }

    @Override
    public String name() {
        return "symmetry-breaking";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void apply(CpModelContext ctx) {
        if (ctx.explainMode() || !properties.isSymmetryBreaking() || hasLockedAssignments(ctx)) {
            return;
        }
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        int constraints = 0;

        Map<Integer, List<Integer>> taskClasses = classes(index.taskSymmetryClass, index.taskCount());
        for (List<Integer> members : taskClasses.values()) {
            for (int k = 0; k + 1 < members.size(); k++) {
                ctx.checkpoint(k);
                int i = members.get(k);
                int j = members.get(k + 1);
                model.addLessOrEqual(ctx.start(i), ctx.start(j));
                constraints++;
                Literal pi = ctx.presence(i);
                Literal pj = ctx.presence(j);
                if (pi != ctx.trueLit() && pj != ctx.trueLit()) {
                    model.addImplication(pj, pi);
                    constraints++;
                }
            }
        }

        Map<Integer, List<Integer>> resourceClasses = classes(index.resourceSymmetryClass, index.resourceCount());
        for (List<Integer> members : resourceClasses.values()) {
            for (int k = 0; k + 1 < members.size(); k++) {
                ctx.checkpoint(k);
                Literal first = ctx.resourceUsed(members.get(k), true);
                Literal second = ctx.resourceUsed(members.get(k + 1), true);
                model.addImplication(second, first);
                constraints++;
            }
        }
        ctx.count(0, 0, constraints, 0);
    }

    /** Groups the indices of each class (value {@code >= 0}) in ascending index order; classes of one member are dropped. */
    private static Map<Integer, List<Integer>> classes(int[] classOf, int size) {
        Map<Integer, List<Integer>> result = new TreeMap<>();
        if (classOf == null) {
            return result;
        }
        for (int i = 0; i < size && i < classOf.length; i++) {
            if (classOf[i] >= 0) {
                result.computeIfAbsent(classOf[i], key -> new ArrayList<>()).add(i);
            }
        }
        result.values().removeIf(members -> members.size() < 2);
        return result;
    }

    private static boolean hasLockedAssignments(CpModelContext ctx) {
        if (ctx.problem().initialSolution() == null) {
            return false;
        }
        for (InitialAssignment assignment : ctx.problem().initialSolution().assignments()) {
            if (assignment.locked()) {
                return true;
            }
        }
        return false;
    }
}

package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CircuitConstraint;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Task;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.cpsat.contrib.ContribSupport;
import eu.isygoit.solver.family.FamilySupport;
import eu.isygoit.solver.index.CandidateIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structural module that models sequence dependent setup / transition times with one {@code AddCircuit} per resource
 * (design 6.6, D-13); {@link #order()} 700, run after the structural model and before the catalogue constraints.
 * <p>
 * For a UNARY resource with a setup matrix: node 0 is the depot, nodes 1..n the candidates of the resource; the depot
 * self-loop means "resource unused" ({@code ctx.resourceUsed(r, true)}), a task self-loop means "task not on this resource",
 * arcs {@code 0 -> i} / {@code i -> 0} mark first / last, and an arc {@code i -> j} (literal {@code a_ij}) enforces
 * {@code start_j >= start_i + duration_i + setup(i, j)}. An arc is only created when {@code estStart_i + duration_i + setup_ij
 * <= lstStart_j} and the tasks differ. Every arc literal is registered through {@code ctx.registerSetupArcs} for
 * {@code minimize_setup}. The circuit is structural (never conditional): it holds in the solve and the explain pass. The
 * base NoOverlap of the resource stays (redundant, strengthens propagation).
 * <p>
 * Setup semantics ({@code SetupMatrix}): tasks without {@code setupClass} have setup 0 to and from everything; otherwise an
 * explicit entry wins, then {@code sameClassTicks} for equal classes, else {@code defaultTicks}. Resources whose matrix is
 * all zero, with capacity other than 1, or with fewer than two candidates get no circuit. The total arc count is guarded by
 * {@code max-setup-arcs}. Stateless, thread-safe (the context it works on is single-threaded per build); ticks.
 */
@Component
public class SetupCircuitModule implements CpSatStructuralModule {

    /** Fixed order of this module (design 6.1 step 7). */
    public static final int ORDER = 700;

    @Override
    public String name() {
        return "setup_circuit";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void apply(CpModelContext ctx) {
        PlanningProblem problem = ctx.problem();
        if (problem.setupMatrices().isEmpty()) {
            return;
        }
        CandidateIndex index = ctx.index();
        CpModel model = ctx.model();
        Map<String, SetupMatrix> matrices = new HashMap<>();
        for (SetupMatrix m : problem.setupMatrices()) {
            matrices.put(m.id(), m);
        }
        int[][] byResource = FamilySupport.candidatesByResource(index);
        long budget = index.limits().getMaxSetupArcs();
        long arcsSoFar = 0;
        for (int r = 0; r < index.resourceCount(); r++) {
            ctx.checkpoint();
            Resource resource = problem.resources().get(r);
            SetupMatrix matrix = resource.setupMatrixId() == null ? null : matrices.get(resource.setupMatrixId());
            int[] cands = byResource[r];
            if (matrix == null || allZero(matrix) || index.capacity(r) != 1 || cands.length < 2) {
                continue;
            }
            Map<String, Integer> explicit = explicitEntries(matrix);
            int n = cands.length;
            List<int[]> feasible = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int ti = index.candidateTask(cands[i]);
                for (int j = 0; j < n; j++) {
                    int tj = index.candidateTask(cands[j]);
                    if (i == j || ti == tj) {
                        continue;
                    }
                    int s = ticks(matrix, explicit, problem.tasks().get(ti), problem.tasks().get(tj));
                    long earliestEnd = (long) index.estStart(ti) + index.candidateDuration(cands[i]) + s;
                    if (earliestEnd <= index.lstStart(tj)) {
                        feasible.add(new int[]{i, j, s});
                        if (arcsSoFar + feasible.size() > budget) {
                            throw new LimitExceededException("max-setup-arcs", arcsSoFar + feasible.size(), budget);
                        }
                    }
                }
                if (i % 256 == 0) {
                    ctx.checkpoint();
                }
            }
            arcsSoFar += feasible.size();
            buildCircuit(ctx, model, index, r, cands, feasible);
        }
    }

    private static void buildCircuit(CpModelContext ctx, CpModel model, CandidateIndex index, int r, int[] cands,
                                     List<int[]> feasible) {
        int n = cands.length;
        CircuitConstraint circuit = model.addCircuit();
        circuit.addArc(0, 0, ctx.resourceUsed(r, true).not());
        for (int i = 0; i < n; i++) {
            int node = i + 1;
            circuit.addArc(node, node, ctx.cand(cands[i]).not());
            circuit.addArc(0, node, model.newBoolVar(""));
            circuit.addArc(node, 0, model.newBoolVar(""));
        }
        List<SetupArc> arcs = new ArrayList<>(feasible.size());
        for (int[] pair : feasible) {
            int i = pair[0];
            int j = pair[1];
            BoolVar arc = model.newBoolVar("");
            circuit.addArc(i + 1, j + 1, arc);
            int s = pair[2];
            int ti = index.candidateTask(cands[i]);
            int tj = index.candidateTask(cands[j]);
            LinearExpr earliest = LinearExpr.affine(ctx.start(ti), 1, (long) index.candidateDuration(cands[i]) + s);
            ContribSupport.enforce(model.addGreaterOrEqual(ctx.start(tj), earliest), arc);
            arcs.add(new SetupArc(arc, s));
        }
        ctx.registerSetupArcs(r, arcs);
        ContribSupport.count(ctx, 2L * n + feasible.size(), 0, feasible.size() + 1L, 0);
    }

    private static boolean allZero(SetupMatrix m) {
        if (m.defaultTicks() > 0 || m.sameClassTicks() > 0) {
            return false;
        }
        for (SetupEntry e : m.entries()) {
            if (e.ticks() > 0) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> explicitEntries(SetupMatrix m) {
        Map<String, Integer> out = new HashMap<>();
        for (SetupEntry e : m.entries()) {
            out.put(key(e.from(), e.to()), e.ticks());
        }
        return out;
    }

    private static String key(String from, String to) {
        return from + '\u0000' + to;
    }

    private static int ticks(SetupMatrix matrix, Map<String, Integer> explicit, Task from, Task to) {
        String a = from.setupClass();
        String b = to.setupClass();
        if (a == null || b == null) {
            return 0;
        }
        Integer entry = explicit.get(key(a, b));
        if (entry != null) {
            return entry;
        }
        return a.equals(b) ? matrix.sameClassTicks() : matrix.defaultTicks();
    }
}

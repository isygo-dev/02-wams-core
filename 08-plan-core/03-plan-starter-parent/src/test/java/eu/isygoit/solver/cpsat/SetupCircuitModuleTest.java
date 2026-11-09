package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Task;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.cpsat.contrib.ContribFixture;
import eu.isygoit.solver.family.FamilyKit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G14: three tasks (A, A, B classes) on one unary machine, setup 5 between different classes. */
class SetupCircuitModuleTest {

    private final SetupCircuitModule module = new SetupCircuitModule();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(),
                "OR-Tools natives not available");
    }

    private static Task task(String id, String setupClass) {
        return new Task(id, null, null, 10, null, null, null, null, null, null, null, null, null, null, null, null,
                setupClass, new LinkedHashSet<>(), null, null);
    }

    private static Resource machine(int capacity) {
        return new Resource("m", null, null, capacity, null, null, null, null, null, null, null, "mat", null);
    }

    private PlanningProblem problem(int horizon, int capacity, SetupMatrix matrix) {
        return new PlanningProblem(1L, "g14", "UTC", FamilyKit.horizon(horizon, 60), null, null, List.of(matrix),
                List.of(machine(capacity)), List.of(task("A1", "A"), task("A2", "A"), task("B", "B")), List.of(), List.of(),
                null, null);
    }

    private ContribFixture fixture(PlanningProblem p, String... optional) {
        return ContribFixture.of(FamilyKit.View.of(p).allCandidates(), optional);
    }

    @Test
    void identity() {
        assertEquals(700, module.order());
        assertEquals("setup_circuit", module.name());
    }

    @Test
    void makespanIsThirtyFiveAndTheSetupTotalIsFive() {
        ContribFixture f = fixture(problem(100, 1, new SetupMatrix("mat", 5, 0, List.of())));
        module.apply(f.ctx);
        List<SetupArc> arcs = f.setupArcsOf(0);
        assertEquals(6, arcs.size());          // 3 * 2 ordered pairs
        IntVar makespan = f.model.newIntVar(0, 100, "makespan");
        for (int t = 0; t < 3; t++) {
            f.model.addGreaterOrEqual(makespan, LinearExpr.affine(f.start[t], 1, 10));
        }
        ContribFixture.Solved solved = f.solve(LinearExpr.term(makespan, 1));
        assertTrue(solved.solved());
        assertEquals(35, Math.round(solved.objective()));
        long setupTotal = 0;
        for (SetupArc arc : arcs) {
            if (solved.solver.booleanValue(arc.literal())) {
                setupTotal += arc.ticks();
            }
        }
        assertEquals(5, setupTotal);
        assertEquals(10, Math.abs(solved.startOf("A1") - solved.startOf("A2")), "the A tasks must be adjacent");
    }

    @Test
    void explicitEntriesOverrideTheDefault() {
        // B -> A costs 20, everything else 0 (same class and default): best is A, A, B with total 0 and makespan 30
        SetupMatrix matrix = new SetupMatrix("mat", 0, 0, List.of(new SetupEntry("B", "A", 20), new SetupEntry("A", "B", 7)));
        ContribFixture f = fixture(problem(100, 1, matrix));
        module.apply(f.ctx);
        IntVar makespan = f.model.newIntVar(0, 100, "makespan");
        for (int t = 0; t < 3; t++) {
            f.model.addGreaterOrEqual(makespan, LinearExpr.affine(f.start[t], 1, 10));
        }
        ContribFixture.Solved solved = f.solve(LinearExpr.term(makespan, 1));
        assertEquals(37, Math.round(solved.objective()));        // A, A, B pays A->B = 7
        assertTrue(solved.startOf("B") > solved.startOf("A1") && solved.startOf("B") > solved.startOf("A2"));
    }

    @Test
    void tooTightAHorizonIsInfeasible() {
        ContribFixture f = fixture(problem(34, 1, new SetupMatrix("mat", 5, 0, List.of())));
        module.apply(f.ctx);
        assertTrue(f.solve(null).infeasible());
        ContribFixture ok = fixture(problem(35, 1, new SetupMatrix("mat", 5, 0, List.of())));
        module.apply(ok.ctx);
        assertTrue(ok.solve(null).solved());
    }

    @Test
    void anAbsentOptionalTaskIsASelfLoop() {
        ContribFixture f = fixture(problem(100, 1, new SetupMatrix("mat", 5, 0, List.of())), "B");
        module.apply(f.ctx);
        f.model.addEquality(f.presence[2], 0);
        IntVar makespan = f.model.newIntVar(0, 100, "makespan");
        f.model.addGreaterOrEqual(makespan, LinearExpr.affine(f.start[0], 1, 10));
        f.model.addGreaterOrEqual(makespan, LinearExpr.affine(f.start[1], 1, 10));
        ContribFixture.Solved solved = f.solve(LinearExpr.term(makespan, 1));
        assertEquals(20, Math.round(solved.objective()));        // A, A back to back, no setup
    }

    @Test
    void allZeroMatrixAndNonUnaryResourcesGetNoCircuit() {
        ContribFixture zero = fixture(problem(100, 1, new SetupMatrix("mat", 0, 0, List.of())));
        module.apply(zero.ctx);
        assertTrue(zero.setupArcsOf(0).isEmpty());

        ContribFixture pool = fixture(problem(100, 2, new SetupMatrix("mat", 5, 0, List.of())));
        module.apply(pool.ctx);
        assertTrue(pool.setupArcsOf(0).isEmpty());
    }

    @Test
    void tasksWithoutAClassHaveNoSetup() {
        PlanningProblem p = new PlanningProblem(1L, "g14", "UTC", FamilyKit.horizon(100, 60), null, null,
                List.of(new SetupMatrix("mat", 5, 0, List.of())), List.of(machine(1)),
                List.of(task("A1", "A"), task("N1", null), task("N2", null)), List.of(), List.of(), null, null);
        ContribFixture f = fixture(p);
        module.apply(f.ctx);
        IntVar makespan = f.model.newIntVar(0, 100, "makespan");
        for (int t = 0; t < 3; t++) {
            f.model.addGreaterOrEqual(makespan, LinearExpr.affine(f.start[t], 1, 10));
        }
        assertEquals(30, Math.round(f.solve(LinearExpr.term(makespan, 1)).objective()));
    }

    @Test
    void theArcBudgetIsGuarded() {
        PlanningProblem p = problem(100, 1, new SetupMatrix("mat", 5, 0, List.of()));
        ContribFixture f = fixture(p);
        f.view.maxSetupArcs(3);
        assertThrows(LimitExceededException.class, () -> module.apply(f.ctx));
    }
}

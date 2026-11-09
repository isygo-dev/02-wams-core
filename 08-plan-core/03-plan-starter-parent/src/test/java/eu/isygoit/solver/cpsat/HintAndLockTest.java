package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.CompiledObjective;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.cpsat.CpSatTestKit.any;
import static eu.isygoit.solver.cpsat.CpSatTestKit.assignment;
import static eu.isygoit.solver.cpsat.CpSatTestKit.at;
import static eu.isygoit.solver.cpsat.CpSatTestKit.options;
import static eu.isygoit.solver.cpsat.CpSatTestKit.resource;
import static eu.isygoit.solver.cpsat.CpSatTestKit.task;
import static eu.isygoit.solver.cpsat.CpSatTestKit.tier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hints and locked assignments (design 6.4 step 6, 6.11): locked assignments are frozen (start and resource) and a
 * conflicting lock makes the problem INFEASIBLE; hints are advisory (a good hint is accepted, an invalid one is ignored and
 * never changes the optimum). Skipped without the OR-Tools natives.
 */
class HintAndLockTest {

    private static final CompiledObjective MAKESPAN = CpSatTestKit.objective(0, CpSatTestKit.MakespanContributor.TYPE, 1, 0);

    private final CpSatSolverEngine engine =
            CpSatTestKit.newEngine(CpSatTestKit.constraints(), CpSatTestKit.objectives(), new CpSatProperties());

    @BeforeAll
    static void requireNatives() {
        Assumptions.assumeTrue(CpSatTestKit.nativesAvailable(), "OR-Tools natives not available");
    }

    private SolveResult solve(int resources, List<eu.isygoit.dto.request.Task> tasks, InitialSolution initial) {
        List<eu.isygoit.dto.request.Resource> pool = new java.util.ArrayList<>();
        for (int i = 1; i <= resources; i++) {
            pool.add(resource("R" + i, 1));
        }
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(), List.of(), pool, tasks, options(10, 1, true), initial);
        return engine.solve(CpSatTestKit.compile(problem, List.of(), List.of(MAKESPAN)),
                CpSatTestKit.context(problem.options(), null));
    }

    @Test
    void aLockedAssignmentKeepsItsStartAndResource() {
        InitialSolution locked = new InitialSolution(List.of(new InitialAssignment("T1", at(7), List.of("R2"), true)));
        SolveResult result = solve(2, List.of(task("T1", 5, true, null, List.of(any(1)), List.of()),
                task("T2", 5, true, null, List.of(any(1)), List.of())), locked);
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status(), String.valueOf(result.diagnostics()));
        Assignment t1 = assignment(result, "T1");
        assertEquals(7, t1.startTick());
        assertEquals("R2", t1.resources().get(0).resourceId());
        assertEquals(12L, tier(result, 0).value(), "makespan is bounded by the locked task: 7 + 5");
    }

    @Test
    void conflictingLocksAreInfeasible() {
        InitialSolution locked = new InitialSolution(List.of(
                new InitialAssignment("T1", at(3), List.of("R1"), true),
                new InitialAssignment("T2", at(5), List.of("R1"), true)));
        SolveResult result = solve(1, List.of(task("T1", 5, true, null, List.of(any(1)), List.of()),
                task("T2", 5, true, null, List.of(any(1)), List.of())), locked);
        assertEquals(IEnumSolverStatus.Types.INFEASIBLE, result.status());
        assertNull(result.solution());
    }

    @Test
    void aGoodHintIsAcceptedAndTheOptimumIsUnchanged() {
        InitialSolution hint = new InitialSolution(List.of(
                new InitialAssignment("T1", at(0), List.of("R1"), false),
                new InitialAssignment("T2", at(5), List.of("R1"), false)));
        SolveResult result = solve(1, List.of(task("T1", 5, true, null, List.of(any(1)), List.of()),
                task("T2", 5, true, null, List.of(any(1)), List.of())), hint);
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status());
        assertEquals(10L, tier(result, 0).value());
        assertTrue(result.metrics().search().solutionsFound() >= 1);
    }

    @Test
    void anInvalidHintIsIgnored() {
        InitialSolution hint = new InitialSolution(List.of(
                new InitialAssignment("T1", at(1000), List.of("R9"), false),
                new InitialAssignment("unknown-task", at(0), List.of("R1"), false)));
        SolveResult result = solve(1, List.of(task("T1", 5, true, null, List.of(any(1)), List.of()),
                task("T2", 5, true, null, List.of(any(1)), List.of())), hint);
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status(), String.valueOf(result.diagnostics()));
        assertEquals(10L, tier(result, 0).value());
    }
}

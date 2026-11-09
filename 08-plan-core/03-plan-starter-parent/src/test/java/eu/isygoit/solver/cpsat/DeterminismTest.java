package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.CompiledObjective;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.cpsat.CpSatTestKit.on;
import static eu.isygoit.solver.cpsat.CpSatTestKit.options;
import static eu.isygoit.solver.cpsat.CpSatTestKit.resource;
import static eu.isygoit.solver.cpsat.CpSatTestKit.task;
import static eu.isygoit.solver.cpsat.CpSatTestKit.tier;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Deterministic mode (design 6.12, golden scenario G13): the 3x2 job shop of design example (b) solved twice with
 * {@code deterministic = true} (single worker and four workers with interleaved search) yields identical assignments
 * and the proven makespan 75. Skipped without the OR-Tools natives.
 */
class DeterminismTest {

    private static final CompiledObjective MAKESPAN = CpSatTestKit.objective(0, CpSatTestKit.MakespanContributor.TYPE, 1, 0);

    private final CpSatSolverEngine engine =
            CpSatTestKit.newEngine(CpSatTestKit.constraints(), CpSatTestKit.objectives(), new CpSatProperties());

    @BeforeAll
    static void requireNatives() {
        Assumptions.assumeTrue(CpSatTestKit.nativesAvailable(), "OR-Tools natives not available");
    }

    private static PlanningProblem jobShop(int workers) {
        List<Task> tasks = List.of(
                task("A1", 30, true, null, List.of(on("M1")), List.of()),
                task("A2", 20, true, null, List.of(on("M2")), List.of(new PrecedenceRef("A1", null, 0, null, null))),
                task("B1", 25, true, null, List.of(on("M2")), List.of()),
                task("B2", 15, true, null, List.of(on("M1")), List.of(new PrecedenceRef("B1", null, 0, null, null))),
                task("C1", 20, true, null, List.of(on("M1")), List.of()),
                task("C2", 30, true, null, List.of(on("M2")), List.of(new PrecedenceRef("C1", null, 0, null, null))));
        return CpSatTestKit.problem(480, List.of(resource("M1", 1), resource("M2", 1)), tasks,
                options(20, workers, true));
    }

    private SolveResult solve(int workers) {
        PlanningProblem problem = jobShop(workers);
        return engine.solve(CpSatTestKit.compile(problem, List.of(), List.of(MAKESPAN)),
                CpSatTestKit.context(problem.options(), null));
    }

    @Test
    void singleWorkerRunsAreIdentical() {
        SolveResult first = solve(1);
        SolveResult second = solve(1);
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, first.status(), String.valueOf(first.diagnostics()));
        assertEquals(75L, tier(first, 0).value());
        assertEquals(first.solution().assignments(), second.solution().assignments());
    }

    @Test
    void multiWorkerRunsAreIdenticalWithInterleavedSearch() {
        SolveResult first = solve(4);
        SolveResult second = solve(4);
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, first.status(), String.valueOf(first.diagnostics()));
        assertEquals(75L, tier(first, 0).value());
        assertEquals(first.solution().assignments(), second.solution().assignments());
        assertEquals(4, first.metrics().workersUsed());
    }
}

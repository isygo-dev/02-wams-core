package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static eu.isygoit.solver.cpsat.CpSatTestKit.on;
import static eu.isygoit.solver.cpsat.CpSatTestKit.options;
import static eu.isygoit.solver.cpsat.CpSatTestKit.resource;
import static eu.isygoit.solver.cpsat.CpSatTestKit.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cancellation and time limit (design 6.11, golden scenario G7): a seeded 30 jobs x 15 machines job shop is generated in
 * code (too hard to solve in the test time), the token is cancelled after two seconds and the engine must return within five
 * seconds with the incumbent ({@code STOPPED_FEASIBLE}; {@code UNKNOWN} is only allowed when no incumbent was found).
 * Skipped without the OR-Tools natives.
 */
class CancellationTest {

    private static final CompiledObjective MAKESPAN = CpSatTestKit.objective(0, CpSatTestKit.MakespanContributor.TYPE, 1, 0);

    private final CpSatSolverEngine engine =
            CpSatTestKit.newEngine(CpSatTestKit.constraints(), CpSatTestKit.objectives(), new CpSatProperties());

    @BeforeAll
    static void requireNatives() {
        Assumptions.assumeTrue(CpSatTestKit.nativesAvailable(), "OR-Tools natives not available");
    }

    /** The G7 generator: {@code jobs} jobs, each visiting every machine once in a seeded random order. */
    static PlanningProblem bigJobShop(int jobs, int machines, long seed, int maxSeconds) {
        Random random = new Random(seed);
        List<Resource> resources = new ArrayList<>();
        for (int m = 0; m < machines; m++) {
            resources.add(resource("M" + m, 1));
        }
        List<Task> tasks = new ArrayList<>();
        for (int j = 0; j < jobs; j++) {
            List<Integer> order = new ArrayList<>();
            for (int m = 0; m < machines; m++) {
                order.add(m);
            }
            Collections.shuffle(order, random);
            String previous = null;
            for (int k = 0; k < machines; k++) {
                String id = "j" + j + "_" + k;
                List<PrecedenceRef> predecessors = previous == null
                        ? List.of() : List.of(new PrecedenceRef(previous, null, 0, null, null));
                tasks.add(task(id, 1 + random.nextInt(20), true, null, List.of(on("M" + order.get(k))), predecessors));
                previous = id;
            }
        }
        return CpSatTestKit.problem(8000, resources, tasks, options(maxSeconds, 4, false));
    }

    @Test
    void aStopAfterTwoSecondsReturnsTheIncumbentQuickly() throws Exception {
        PlanningProblem problem = bigJobShop(30, 15, 7L, 60);
        CompiledProblem compiled = CpSatTestKit.compile(problem, List.of(), List.of(MAKESPAN));
        SolverCancellationToken token = new SolverCancellationToken();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<SolveResult> future = executor.submit(() -> engine.solve(compiled,
                    new SolveContext(problem.options(), token, SolveListener.NOOP, System.nanoTime())));
            Thread.sleep(2000);
            long cancelledAt = System.nanoTime();
            token.cancel();
            SolveResult result = future.get(30, TimeUnit.SECONDS);
            long afterCancelMillis = (System.nanoTime() - cancelledAt) / 1_000_000L;

            assertTrue(afterCancelMillis < 5000, "the engine must return within 5 s of the stop, took " + afterCancelMillis);
            if (result.status() == IEnumSolverStatus.Types.STOPPED_FEASIBLE) {
                assertNotNull(result.solution());
                assertTrue(result.metrics().search().solutionsFound() >= 1);
                assertEquals(450, result.solution().assignments().size());
            } else {
                assertEquals(IEnumSolverStatus.Types.UNKNOWN, result.status(), String.valueOf(result.diagnostics()));
                assertNull(result.solution());
                assertEquals(0, result.metrics().search().solutionsFound(), "UNKNOWN is only allowed without an incumbent");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aStopRequestedBeforeTheBuildReturnsUnknownImmediately() {
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("M", 1)), List.of(task("a", 5), task("b", 5)),
                options(10, 1, true));
        CompiledProblem compiled = CpSatTestKit.compile(problem, List.of(), List.of(MAKESPAN));
        SolverCancellationToken token = new SolverCancellationToken();
        token.cancel();
        long started = System.nanoTime();
        SolveResult result = engine.solve(compiled, CpSatTestKit.context(problem.options(), token));
        assertEquals(IEnumSolverStatus.Types.UNKNOWN, result.status());
        assertNull(result.solution());
        assertEquals(SolveCancelledException.STOP_REQUESTED, result.diagnostics().message());
        assertTrue((System.nanoTime() - started) / 1_000_000L < 2000);
    }

    @Test
    void theTimeLimitIsHonouredAndReportedAsFeasibleOrUnknown() {
        PlanningProblem problem = bigJobShop(30, 15, 11L, 2);
        CompiledProblem compiled = CpSatTestKit.compile(problem, List.of(), List.of(MAKESPAN));
        long started = System.nanoTime();
        SolveResult result = engine.solve(compiled, CpSatTestKit.context(problem.options(), null));
        long millis = (System.nanoTime() - started) / 1_000_000L;
        assertTrue(millis < 10_000, "a 2 s budget must not run for " + millis + " ms");
        assertTrue(result.status() == IEnumSolverStatus.Types.FEASIBLE
                        || result.status() == IEnumSolverStatus.Types.UNKNOWN,
                "a 450-operation job shop is not proven optimal in 2 s: " + result.status());
    }
}

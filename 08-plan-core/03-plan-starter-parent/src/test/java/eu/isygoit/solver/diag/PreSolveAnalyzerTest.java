package eu.isygoit.solver.diag;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.check.StubIndexView;
import eu.isygoit.solver.check.TestProblems;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static eu.isygoit.solver.check.TestProblems.at;
import static eu.isygoit.solver.check.TestProblems.requirement;
import static eu.isygoit.solver.check.TestProblems.requirementOn;
import static eu.isygoit.solver.check.TestProblems.resource;
import static eu.isygoit.solver.check.TestProblems.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreSolveAnalyzerTest {

    private final PreSolveAnalyzer analyzer = new PreSolveAnalyzer();

    private Optional<Diagnostics> analyze(PlanningProblem problem) {
        return analyze(problem, StubIndexView.of(problem));
    }

    private Optional<Diagnostics> analyze(PlanningProblem problem, StubIndexView index) {
        return analyzer.analyze(problem, index, index);
    }

    private static List<CoreItem> items(Diagnostics d, String reason) {
        return d.core().items().stream().filter(i -> reason.equals(i.reasonCode())).toList();
    }

    private static PlanningProblem problem(int horizon, List<Resource> resources, List<Task> tasks) {
        return TestProblems.problem(horizon, resources, tasks, List.of(), List.of());
    }

    @Test
    void feasibleProblemYieldsNothing() {
        assertTrue(analyze(problem(100, List.of(resource("R", 1)),
                List.of(task("A", 10).build(), task("B", 10).after("A").build()))).isEmpty());
    }

    @Test
    void g2TaskWithoutAnyCandidateIsProvenInfeasibleWithTheReason() {
        PlanningProblem p = problem(100, List.of(resource("R", 1, "milling")), List.of(
                task("ok", 10).build(),
                task("weld", 10).requires(requirement("default", 1, null, "welding")).build()));
        Diagnostics d = analyze(p).orElseThrow();
        assertEquals(CoreItems.METHOD_PRESOLVE, d.core().method());
        assertTrue(d.core().minimal());
        CoreItem item = items(d, CoreItems.NO_ELIGIBLE_RESOURCE).get(0);
        assertEquals(CoreItems.TASK, item.kind());
        assertEquals("weld", item.id());
        assertTrue(item.message().contains("SKILL"), item.message());
        assertEquals(List.of("weld"), d.infeasibilityCore());
        assertTrue(d.message().contains("weld"));
    }

    @Test
    void capacityAmountAndPruningConstraintsAreNamed() {
        PlanningProblem amount = problem(100, List.of(resource("R", 1)),
                List.of(task("big", 5).demand(3).build()));
        assertTrue(items(analyze(amount).orElseThrow(), CoreItems.NO_ELIGIBLE_RESOURCE).get(0).message()
                .contains("CAPACITY_AMOUNT"));

        Constraint eligibility = new Constraint("elig", "task_eligibility", IEnumEnforcement.Types.STRICTLY_REQUIRED,
                new ConstraintScope("task", "A"), Map.of(), null, 0, null);
        PlanningProblem pruned = TestProblems.problem(100, List.of(resource("R", 1)),
                List.of(task("A", 5).build()), List.of(eligibility), List.of());
        StubIndexView index = StubIndexView.of(pruned).prune("A", "R");
        Diagnostics d = analyze(pruned, index).orElseThrow();
        assertTrue(items(d, CoreItems.NO_ELIGIBLE_RESOURCE).get(0).message().contains("ELIGIBILITY:elig"));
        assertEquals(List.of("elig", "A"), d.infeasibilityCore());
        assertEquals(CoreItems.CONSTRAINT, d.core().items().get(0).kind());
    }

    @Test
    void requirementCountLargerThanCandidates() {
        PlanningProblem p = problem(100, List.of(resource("R1", 1), resource("R2", 1)),
                List.of(task("crew", 5).requires(requirement("default", 3, null)).build()));
        Diagnostics d = analyze(p).orElseThrow();
        assertEquals("crew", items(d, CoreItems.REQUIREMENT_COUNT).get(0).id());
    }

    @Test
    void windowShorterThanDurationIsEmpty() {
        PlanningProblem p = problem(100, List.of(resource("R", 1)),
                List.of(task("tight", 10).release(0).deadline(5).build()));
        Diagnostics d = analyze(p).orElseThrow();
        assertEquals("tight", items(d, CoreItems.WINDOW_EMPTY).get(0).id());
    }

    @Test
    void emptyStartDomainFromTheIndexIsReported() {
        PlanningProblem p = problem(100, List.of(resource("R", 1)), List.of(task("A", 10).build()));
        StubIndexView index = StubIndexView.of(p).window("A", 60, 50);
        assertEquals("A", items(analyze(p, index).orElseThrow(), CoreItems.WINDOW_EMPTY).get(0).id());
    }

    @Test
    void criticalPathReportsOnlyTheRootCauseWithItsEdges() {
        PlanningProblem p = problem(200, List.of(resource("R1", 1), resource("R2", 1), resource("R3", 1)), List.of(
                task("A", 10).build(),
                task("B", 10).after("A").build(),
                task("C", 10).after("B").deadline(25).build(),
                task("D", 10).after("C").deadline(28).build()));
        Diagnostics d = analyze(p).orElseThrow();
        List<CoreItem> path = items(d, CoreItems.CRITICAL_PATH_TOO_LONG);
        assertEquals(1, path.size(), "D is a consequence of C and must not be reported again");
        assertEquals("C", path.get(0).id());
        List<String> edges = d.core().items().stream().filter(i -> CoreItems.EDGE.equals(i.kind()))
                .map(CoreItem::id).toList();
        assertEquals(List.of("A->B", "B->C"), edges);
        assertTrue(items(d, CoreItems.WINDOW_EMPTY).isEmpty());
    }

    @Test
    void criticalPathHonoursLagsAndStartStartEdges() {
        PlanningProblem p = problem(200, List.of(resource("R1", 1), resource("R2", 1)), List.of(
                task("A", 10).build(),
                task("B", 10).after("A", eu.isygoit.dto.request.PrecedenceRef.Type.START_START, 30, null,
                        eu.isygoit.dto.request.PrecedenceRef.Presence.WHEN_BOTH).deadline(35).build()));
        Diagnostics d = analyze(p).orElseThrow();
        assertEquals("B", items(d, CoreItems.CRITICAL_PATH_TOO_LONG).get(0).id());
        PlanningProblem fits = problem(200, List.of(resource("R1", 1), resource("R2", 1)), List.of(
                task("A", 10).build(),
                task("B", 10).after("A", eu.isygoit.dto.request.PrecedenceRef.Type.START_START, 30, null,
                        eu.isygoit.dto.request.PrecedenceRef.Presence.WHEN_BOTH).deadline(40).build()));
        assertTrue(analyze(fits).isEmpty());
    }

    @Test
    void optionalTasksAreIgnoredByTheCriticalPath() {
        PlanningProblem p = problem(200, List.of(resource("R", 1)), List.of(
                task("A", 100).optional(1L).build(),
                task("B", 10).after("A").deadline(50).build()));
        assertTrue(analyze(p).isEmpty(), "WHEN_BOTH edge from an optional task does not force the predecessor");
    }

    @Test
    void impliesEdgeForcesTheOptionalPredecessor() {
        PlanningProblem p = problem(200, List.of(resource("R", 1)), List.of(
                task("A", 100).optional(1L).build(),
                task("B", 10).after("A", eu.isygoit.dto.request.PrecedenceRef.Type.FINISH_START, 0, null,
                        eu.isygoit.dto.request.PrecedenceRef.Presence.IMPLIES).deadline(50).build()));
        assertEquals("B", items(analyze(p).orElseThrow(), CoreItems.CRITICAL_PATH_TOO_LONG).get(0).id());
    }

    @Test
    void forcedWorkAboveTheAvailableEnergyOfOneResource() {
        PlanningProblem p = problem(100, List.of(resource("R", 1), resource("S", 1)), List.of(
                task("A", 30).deadline(50).requires(requirementOn("default", "R")).build(),
                task("B", 30).deadline(50).requires(requirementOn("default", "R")).build(),
                task("C", 30).deadline(50).build()));
        Diagnostics d = analyze(p).orElseThrow();
        CoreItem resource = items(d, CoreItems.CAPACITY_EXCEEDED).get(0);
        assertEquals(CoreItems.RESOURCE, d.core().items().stream()
                .filter(i -> "R".equals(i.id())).findFirst().orElseThrow().kind());
        assertTrue(items(d, CoreItems.CAPACITY_EXCEEDED).stream().anyMatch(i -> "A".equals(i.id())));
        assertTrue(items(d, CoreItems.CAPACITY_EXCEEDED).stream().noneMatch(i -> "C".equals(i.id())),
                "C has an alternative resource and is not forced");
        assertFalse(resource.message().isEmpty());
    }

    @Test
    void unavailableWindowsReduceTheEnergy() {
        Resource limited = TestProblems.resourceWith("R", 1, List.of(new TimeWindow(at(20), at(40))), null, null);
        PlanningProblem p = problem(60, List.of(limited), List.of(
                task("A", 25).build(), task("B", 25).build()));
        // available ticks 40 (60 - 20) < 50 forced
        assertTrue(analyze(p).orElseThrow().core().items().stream().anyMatch(
                i -> CoreItems.RESOURCE.equals(i.kind()) && CoreItems.CAPACITY_EXCEEDED.equals(i.reasonCode())));
    }

    @Test
    void cumulativeResourceUsesCapacityTimesTime() {
        PlanningProblem p = problem(50, List.of(resource("POOL", 2)), List.of(
                task("A", 50).demand(2).build(), task("B", 50).demand(1).build()));
        // energy 100 + 50 = 150 > 2 * 50
        assertTrue(analyze(p).orElseThrow().core().items().stream().anyMatch(
                i -> CoreItems.RESOURCE.equals(i.kind()) && CoreItems.CAPACITY_EXCEEDED.equals(i.reasonCode())));
    }

    @Test
    void maxLoadTicksBelowForcedWork() {
        Resource capped = TestProblems.resourceWith("R", 1, List.of(), 10, null);
        PlanningProblem p = problem(100, List.of(capped), List.of(task("A", 8).build(), task("B", 8).build()));
        Diagnostics d = analyze(p).orElseThrow();
        assertTrue(d.core().items().stream().anyMatch(i -> "R".equals(i.id())
                && i.message().contains("maxLoadTicks")));
    }

    @Test
    void pinOutsideTheWindowOrOnANonCandidate() {
        PlanningProblem late = problem(100, List.of(resource("R", 1)),
                List.of(task("A", 10).deadline(50).pin(45).build()));
        assertEquals("A", items(analyze(late).orElseThrow(), CoreItems.PIN_CONFLICT).get(0).id());

        PlanningProblem wrongResource = problem(100, List.of(resource("R", 1, "x"), resource("S", 1)),
                List.of(task("A", 10).requires(requirement("default", 1, null, "x")).pin(null, "S").build()));
        assertEquals("A", items(analyze(wrongResource).orElseThrow(), CoreItems.PIN_CONFLICT).get(0).id());
    }

    @Test
    void lockedAssignmentOnANonCandidateOrOutsideTheDomain() {
        PlanningProblem bad = TestProblems.problem(100, List.of(), List.of(),
                List.of(resource("R", 1, "x"), resource("S", 1)),
                List.of(task("A", 10).requires(requirement("default", 1, null, "x")).build()),
                new InitialSolution(List.of(new InitialAssignment("A", at(10), List.of("S"), true))));
        assertEquals("A", items(analyze(bad).orElseThrow(), CoreItems.LOCKED_CONFLICT).get(0).id());

        PlanningProblem outside = TestProblems.problem(100, List.of(), List.of(),
                List.of(resource("R", 1)),
                List.of(task("A", 10).deadline(50).build()),
                new InitialSolution(List.of(new InitialAssignment("A", at(60), List.of("R"), true))));
        assertEquals("A", items(analyze(outside).orElseThrow(), CoreItems.LOCKED_CONFLICT).get(0).id());

        PlanningProblem hint = TestProblems.problem(100, List.of(), List.of(),
                List.of(resource("R", 1)), List.of(task("A", 10).deadline(50).build()),
                new InitialSolution(List.of(new InitialAssignment("A", at(60), List.of("R"), false))));
        assertTrue(analyze(hint).isEmpty(), "a non locked hint is never a proof");
    }

    @Test
    void findingsAreCappedAndTheCoreIsOrdered() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < PreSolveAnalyzer.MAX_FINDINGS + 10; i++) {
            tasks.add(task(String.format("t%03d", i), 5)
                    .requires(requirement("default", 1, null, "none")).build());
        }
        Diagnostics d = analyze(problem(100, List.of(resource("R", 1)), tasks)).orElseThrow();
        assertEquals(PreSolveAnalyzer.MAX_FINDINGS, d.core().items().size());
        assertEquals(true, d.details().get("truncated"));
        assertEquals("t000", d.core().items().get(0).id());
        assertEquals("PRESOLVE", d.details().get("phase"));
    }
}

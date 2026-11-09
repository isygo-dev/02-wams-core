package eu.isygoit.solver.check;

import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.response.Assignment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static eu.isygoit.solver.check.TestProblems.assign;
import static eu.isygoit.solver.check.TestProblems.at;
import static eu.isygoit.solver.check.TestProblems.resource;
import static eu.isygoit.solver.check.TestProblems.resourceWith;
import static eu.isygoit.solver.check.TestProblems.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralSolutionCheckerTest {

    private static List<StructuralSolutionChecker.Violation> check(PlanningProblem problem, Assignment... assignments) {
        StubIndexView index = StubIndexView.of(problem);
        SolutionViewImpl view = SolutionViewImpl.build(index, List.of(assignments));
        return new StructuralSolutionChecker(problem, index).check(view);
    }

    private static Set<String> codes(List<StructuralSolutionChecker.Violation> violations) {
        return violations.stream().map(StructuralSolutionChecker.Violation::code).collect(Collectors.toSet());
    }

    private static PlanningProblem simple(List<Resource> resources, List<Task> tasks) {
        return TestProblems.problem(100, resources, tasks, List.of(), List.of());
    }

    @Test
    void validSequentialPlanHasNoViolation() {
        PlanningProblem p = simple(List.of(resource("R", 1)),
                List.of(task("A", 10).build(), task("B", 5).build()));
        assertTrue(check(p, assign("A", 0, 10, "R"), assign("B", 10, 15, "R")).isEmpty());
    }

    @Test
    void unarySweepDetectsOverlapEvenWithAnEarlierLongTask() {
        PlanningProblem p = simple(List.of(resource("R", 1)),
                List.of(task("A", 30).build(), task("B", 2).build(), task("C", 2).build()));
        // B is inside A, C starts after B ended but while A still runs: only the running maximum end catches it
        List<StructuralSolutionChecker.Violation> v = check(p,
                assign("A", 0, 30, "R"), assign("B", 5, 7, "R"), assign("C", 10, 12, "R"));
        assertEquals(Set.of("SOLUTION_INVALID_OVERLAP"), codes(v));
        assertEquals(2, v.size());
    }

    @Test
    void touchingIntervalsDoNotOverlapAndZeroLengthTasksAreIgnored() {
        PlanningProblem p = simple(List.of(resource("R", 1)),
                List.of(task("A", 10).build(), task("M", 0).build(), task("B", 10).build()));
        assertTrue(check(p, assign("A", 0, 10, "R"), assign("M", 5, 5, "R"), assign("B", 10, 20, "R")).isEmpty());
    }

    @Test
    void cumulativeSweepReleasesEndsBeforeStartsAndRejectsOverload() {
        PlanningProblem p = simple(List.of(resource("R", 2)),
                List.of(task("A", 5).build(), task("B", 5).build(), task("C", 5).build()));
        // A [0,5) B [2,7) C [5,10): at tick 5 A has ended, load stays 2
        assertTrue(check(p, assign("A", 0, 5, "R"), assign("B", 2, 7, "R"), assign("C", 5, 10, "R")).isEmpty());
        // C [4,9) overlaps both A and B: load 3
        List<StructuralSolutionChecker.Violation> v = check(p,
                assign("A", 0, 5, "R"), assign("B", 2, 7, "R"), assign("C", 4, 9, "R"));
        assertEquals(Set.of("SOLUTION_INVALID_CAPACITY"), codes(v));
    }

    @Test
    void cumulativeUsesTheRequirementAmount() {
        PlanningProblem p = simple(List.of(resource("R", 3)),
                List.of(task("A", 10).demand(2).build(), task("B", 10).demand(2).build()));
        List<StructuralSolutionChecker.Violation> v = check(p,
                assign("A", 0, 10, 2, false, "R"), assign("B", 5, 15, 2, false, "R"));
        assertEquals(Set.of("SOLUTION_INVALID_CAPACITY"), codes(v));
        assertTrue(check(p, assign("A", 0, 10, 2, false, "R"), assign("B", 10, 20, 2, false, "R")).isEmpty());
    }

    @Test
    void amountAboveCapacityIsRejected() {
        PlanningProblem p = simple(List.of(resource("R", 2)), List.of(task("A", 5).demand(3).build()));
        assertTrue(codes(check(p, assign("A", 0, 5, 3, false, "R"))).contains("SOLUTION_INVALID_AMOUNT"));
    }

    private static PlanningProblem setupProblem(String... ids) {
        SetupMatrix matrix = new SetupMatrix("m", 5, 0, List.of(new SetupEntry("A", "C", 8)));
        Map<String, Task> all = Map.of(
                "t1", task("t1", 10).setupClass("A").build(),
                "t2", task("t2", 10).setupClass("B").build(),
                "t3", task("t3", 10).setupClass("A").build(),
                "t4", task("t4", 10).setupClass("C").build());
        List<Task> tasks = java.util.Arrays.stream(ids).map(all::get).toList();
        return TestProblems.problem(200, List.of(), List.of(matrix),
                List.of(resourceWith("R", 1, List.of(), null, "m")), tasks, new InitialSolution(null));
    }

    @Test
    void setupGapsAreEnforcedBetweenDifferentClassesOnly() {
        // A -> B needs 5 (default), B -> A needs 5, A -> A needs 0 (same class), A -> C needs 8 (explicit entry)
        assertTrue(check(setupProblem("t1", "t2", "t3", "t4"), assign("t1", 0, 10, "R"),
                assign("t2", 15, 25, "R"), assign("t3", 30, 40, "R"), assign("t4", 48, 58, "R")).isEmpty());
        assertEquals(Set.of("SOLUTION_INVALID_SETUP"), codes(check(setupProblem("t1", "t2"),
                assign("t1", 0, 10, "R"), assign("t2", 13, 23, "R"))));
        assertEquals(Set.of("SOLUTION_INVALID_SETUP"), codes(check(setupProblem("t1", "t4"),
                assign("t1", 0, 10, "R"), assign("t4", 15, 25, "R"))));
        assertTrue(check(setupProblem("t1", "t3"), assign("t1", 0, 10, "R"), assign("t3", 10, 20, "R"))
                .isEmpty());
    }

    @Test
    void unavailableWindowIsRespectedPerRunBoundary() {
        Resource r = resourceWith("R", 1, List.of(new TimeWindow(at(10), at(20))), null, null);
        PlanningProblem p = simple(List.of(r), List.of(task("A", 4).build()));
        assertTrue(check(p, assign("A", 20, 24, "R")).isEmpty());
        assertTrue(check(p, assign("A", 6, 10, "R")).isEmpty());
        assertEquals(Set.of("SOLUTION_INVALID_CALENDAR"), codes(check(p, assign("A", 8, 12, "R"))));
    }

    @Test
    void releaseDeadlineDurationAndHorizonAreChecked() {
        PlanningProblem p = simple(List.of(resource("R", 1)),
                List.of(task("A", 10).release(20).deadline(40).build()));
        assertTrue(check(p, assign("A", 20, 30, "R")).isEmpty());
        assertTrue(codes(check(p, assign("A", 10, 20, "R"))).contains("SOLUTION_INVALID_WINDOW"));
        assertTrue(codes(check(p, assign("A", 35, 45, "R"))).contains("SOLUTION_INVALID_WINDOW"));
        assertTrue(codes(check(p, assign("A", 20, 28, "R"))).contains("SOLUTION_INVALID_DURATION"));
        assertTrue(codes(check(p, assign("A", 95, 105, "R"))).contains("SOLUTION_INVALID_HORIZON"));
    }

    @Test
    void resourceSpecificDurationIsUsed() {
        PlanningProblem p = simple(List.of(resource("R1", 1), resource("R2", 1)),
                List.of(task("A", 10).resourceDurations(Map.of("R2", 20)).build()));
        assertTrue(check(p, assign("A", 0, 10, "R1")).isEmpty());
        assertTrue(check(p, assign("A", 0, 20, "R2")).isEmpty());
        assertTrue(codes(check(p, assign("A", 0, 10, "R2"))).contains("SOLUTION_INVALID_DURATION"));
    }

    @Test
    void eligibilityAndRequirementCountAreChecked() {
        PlanningProblem p = simple(List.of(resource("R1", 1, "weld"), resource("R2", 1)),
                List.of(task("A", 5).requires(TestProblems.requirement("default", 1, null, "weld")).build(),
                        task("B", 5).requires(TestProblems.requirement("default", 2, null)).build()));
        assertTrue(codes(check(p, assign("A", 0, 5, "R2"), assign("B", 10, 15, "R1", "R2")))
                .contains("SOLUTION_INVALID_ELIGIBILITY"));
        assertTrue(codes(check(p, assign("A", 0, 5, "R1"), assign("B", 10, 15, "R1")))
                .contains("SOLUTION_INVALID_REQUIREMENT"));
        assertTrue(check(p, assign("A", 0, 5, "R1"), assign("B", 10, 15, "R1", "R2")).isEmpty());
    }

    @Test
    void missingRequiredTaskAndUnselectedOptionalTask() {
        PlanningProblem p = simple(List.of(resource("R", 1)),
                List.of(task("A", 5).build(), task("O", 5).optional(10L).build()));
        assertEquals(Set.of("SOLUTION_INVALID_TASK_MISSING"), codes(check(p, assign("O", 0, 5, 1, true, "R"))));
        assertTrue(check(p, assign("A", 0, 5, "R")).isEmpty());
    }

    @Test
    void precedenceTypesLagsAndImplies() {
        PlanningProblem p = simple(List.of(resource("R1", 1), resource("R2", 1)), List.of(
                task("A", 10).build(),
                task("B", 5).after("A", PrecedenceRef.Type.FINISH_START, 3, 6, PrecedenceRef.Presence.WHEN_BOTH).build(),
                task("C", 5).after("A", PrecedenceRef.Type.START_START, 2, null, PrecedenceRef.Presence.WHEN_BOTH).build(),
                task("O", 5).optional(1L)
                        .after("A", PrecedenceRef.Type.FINISH_FINISH, 0, null, PrecedenceRef.Presence.IMPLIES).build()));
        // B starts 4 after A ends (within [3,6]); C starts 2 after A starts; O absent (A present: fine)
        assertTrue(check(p, assign("A", 0, 10, "R1"), assign("B", 14, 19, "R1"), assign("C", 2, 7, "R2")).isEmpty());
        assertTrue(codes(check(p, assign("A", 0, 10, "R1"), assign("B", 11, 16, "R1"), assign("C", 2, 7, "R2")))
                .contains("SOLUTION_INVALID_PRECEDENCE"));
        assertTrue(codes(check(p, assign("A", 0, 10, "R1"), assign("B", 17, 22, "R1"), assign("C", 2, 7, "R2")))
                .contains("SOLUTION_INVALID_PRECEDENCE"));
        assertTrue(codes(check(p, assign("A", 0, 10, "R1"), assign("B", 14, 19, "R1"), assign("C", 1, 6, "R2")))
                .contains("SOLUTION_INVALID_PRECEDENCE"));
        // IMPLIES: O selected while A is not -> violation (A is required so it must also be reported missing)
        List<StructuralSolutionChecker.Violation> v = check(p, assign("B", 14, 19, "R1"),
                assign("O", 0, 5, 1, true, "R2"));
        assertTrue(codes(v).contains("SOLUTION_INVALID_PRECEDENCE"));
    }

    @Test
    void slotModeRequiresSlotStartAndFit() {
        PlanningProblem p = TestProblems.problem(100,
                List.of(TestProblems.slot("s1", 10, 20), TestProblems.slot("s2", 30, 40)), List.of(),
                List.of(resource("R", 1)), List.of(task("A", 10).slots("s1", "s2").build()), new InitialSolution(null));
        assertTrue(check(p, assign("A", 30, 40, "R")).isEmpty());
        assertTrue(codes(check(p, assign("A", 11, 21, "R"))).contains("SOLUTION_INVALID_SLOT"));
        assertTrue(codes(check(p, assign("A", 20, 30, "R"))).contains("SOLUTION_INVALID_SLOT"));
    }

    @Test
    void pinAndLockedAssignmentsAreEnforced() {
        PlanningProblem p = TestProblems.problem(100, List.of(), List.of(),
                List.of(resource("R1", 1), resource("R2", 1)),
                List.of(task("A", 5).pin(10, "R1").build(), task("B", 5).build()),
                new InitialSolution(List.of(new InitialAssignment("B", at(40), List.of("R2"), true))));
        assertTrue(check(p, assign("A", 10, 15, "R1"), assign("B", 40, 45, "R2")).isEmpty());
        assertTrue(codes(check(p, assign("A", 11, 16, "R1"), assign("B", 40, 45, "R2")))
                .contains("SOLUTION_INVALID_PIN"));
        assertTrue(codes(check(p, assign("A", 10, 15, "R2"), assign("B", 40, 45, "R2")))
                .contains("SOLUTION_INVALID_PIN"));
        assertEquals(Set.of("SOLUTION_INVALID_LOCKED"),
                codes(check(p, assign("A", 10, 15, "R1"), assign("B", 41, 46, "R2"))));
        assertEquals(Set.of("SOLUTION_INVALID_LOCKED"),
                codes(check(p, assign("A", 10, 15, "R1"), assign("B", 40, 45, "R1"))));
    }

    @Test
    void maxLoadTicksIsEnforced() {
        PlanningProblem p = simple(List.of(resourceWith("R", 1, List.of(), 12, null)),
                List.of(task("A", 8).build(), task("B", 8).build()));
        assertEquals(Set.of("SOLUTION_INVALID_MAX_LOAD"),
                codes(check(p, assign("A", 0, 8, "R"), assign("B", 10, 18, "R"))));
    }

    @Test
    void recordLevelChecksCatchWrongInstantsAndOptionalFlag() {
        PlanningProblem p = simple(List.of(resource("R", 1)),
                List.of(task("A", 5).build(), task("O", 5).optional(1L).build()));
        StubIndexView index = StubIndexView.of(p);
        StructuralSolutionChecker checker = new StructuralSolutionChecker(p, index);
        Assignment wrongOptional = assign("A", 0, 5, 1, true, "R");
        Assignment wrongInstant = new Assignment("O", true, 10, 15, at(11), at(15), null,
                assign("O", 10, 15, "R").resources());
        Set<String> found = checker.checkRecords(List.of(wrongOptional, wrongInstant)).stream()
                .map(StructuralSolutionChecker.Violation::code).collect(Collectors.toSet());
        assertEquals(Set.of("SOLUTION_INVALID_OPTIONAL_FLAG", "SOLUTION_INVALID_INSTANT"), found);
    }

    @Test
    void subtractAndRunLookupHelpers() {
        int[] runs = StructuralSolutionChecker.subtract(new int[]{0, 100},
                List.of(new int[]{40, 50}, new int[]{10, 20}, new int[]{15, 30}));
        assertEquals(List.of(0, 10, 30, 40, 50, 100), java.util.Arrays.stream(runs).boxed().toList());
        assertTrue(StructuralSolutionChecker.insideOneRun(runs, 30, 40));
        assertTrue(!StructuralSolutionChecker.insideOneRun(runs, 35, 45));
        assertTrue(!StructuralSolutionChecker.insideOneRun(runs, 12, 14));
    }
}

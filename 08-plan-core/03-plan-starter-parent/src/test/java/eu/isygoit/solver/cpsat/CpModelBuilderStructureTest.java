package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.Pin;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.isygoit.solver.cpsat.CpSatTestKit.any;
import static eu.isygoit.solver.cpsat.CpSatTestKit.assignment;
import static eu.isygoit.solver.cpsat.CpSatTestKit.at;
import static eu.isygoit.solver.cpsat.CpSatTestKit.on;
import static eu.isygoit.solver.cpsat.CpSatTestKit.options;
import static eu.isygoit.solver.cpsat.CpSatTestKit.resource;
import static eu.isygoit.solver.cpsat.CpSatTestKit.task;
import static eu.isygoit.solver.cpsat.CpSatTestKit.tier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural model of design 6.4 on the real solver: requirement counts, interval models (NoOverlap / Cumulative),
 * calendars, slot start choices, every precedence type, optional tasks, the explain build and the model statistics.
 * Uses tiny in-test contributors (makespan, value, latest end) so it does not depend on the family batches. Skipped when the
 * OR-Tools native library cannot be loaded.
 */
class CpModelBuilderStructureTest {

    private static final CompiledObjective MAKESPAN = CpSatTestKit.objective(0, CpSatTestKit.MakespanContributor.TYPE, 1, 0);
    private static final CompiledObjective VALUE = CpSatTestKit.objective(0, CpSatTestKit.ValueContributor.TYPE, 1, 0);

    private final CpSatSolverEngine engine =
            CpSatTestKit.newEngine(CpSatTestKit.constraints(), CpSatTestKit.objectives(), new CpSatProperties());

    @BeforeAll
    static void requireNatives() {
        Assumptions.assumeTrue(CpSatTestKit.nativesAvailable(), "OR-Tools natives not available");
    }

    private SolveResult solve(PlanningProblem problem, List<CompiledConstraint> constraints,
                              CompiledObjective... objectives) {
        CompiledProblem compiled = CpSatTestKit.compile(problem, constraints, List.of(objectives));
        return engine.solve(compiled, CpSatTestKit.context(problem.options(), null));
    }

    private static void assertOptimal(SolveResult result) {
        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status(), String.valueOf(result.diagnostics()));
        assertNotNull(result.solution());
    }

    // ------------------------------------------------------------------------------------ requirements

    @Test
    void aRequirementWithCountTwoUsesTwoDistinctResources() {
        PlanningProblem problem = CpSatTestKit.problem(30,
                List.of(resource("A", 1), resource("B", 1), resource("C", 1)),
                List.of(task("T1", 5, true, null, List.of(any(2)), List.of())), options(10, 1, true));
        SolveResult result = solve(problem, List.of());
        assertOptimal(result);
        List<ResourceAssignment> used = assignment(result, "T1").resources();
        assertEquals(2, used.size());
        Set<String> ids = new HashSet<>();
        used.forEach(r -> ids.add(r.resourceId()));
        assertEquals(2, ids.size(), "a resource fills at most one slot of the requirement");
        assertEquals("worker", used.get(0).role());
    }

    @Test
    void aRequirementWithCountTwoIsInfeasibleWithOneResource() {
        PlanningProblem problem = CpSatTestKit.problem(30, List.of(resource("A", 1)),
                List.of(task("T1", 5, true, null, List.of(any(2)), List.of())), options(10, 1, true));
        assertEquals(IEnumSolverStatus.Types.INFEASIBLE, solve(problem, List.of()).status());
    }

    // ------------------------------------------------------------------------------------ interval models

    @Test
    void unaryResourcesNeverOverlap() {
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("M", 1)),
                List.of(task("a", 5), task("b", 5), task("c", 5)), options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        assertEquals(15L, tier(result, 0).value());
        List<Assignment> sorted = result.solution().assignments();
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i - 1).endTick() <= sorted.get(i).startTick(), "tasks overlap on a unary resource");
        }
    }

    @Test
    void cumulativeResourcesHonourTheirCapacity() {
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("POOL", 2)),
                List.of(task("a", 4), task("b", 4), task("c", 4)), options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        assertEquals(8L, tier(result, 0).value(), "two tasks in parallel, the third afterwards");
        for (int tick = 0; tick < 40; tick++) {
            int load = 0;
            for (Assignment a : result.solution().assignments()) {
                if (a.startTick() <= tick && tick < a.endTick()) {
                    load++;
                }
            }
            assertTrue(load <= 2, "capacity exceeded at tick " + tick);
        }
    }

    @Test
    void resourceSpecificDurationsFollowTheChosenResource() {
        Task flexible = new Task("T", null, null, 10, java.util.Map.of("FAST", 3), null, null, true, null,
                List.of(any(1)), null, null, null, null, null, List.of(), null, null, null, null);
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("SLOW", 1), resource("FAST", 1)),
                List.of(flexible), options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        Assignment a = assignment(result, "T");
        assertEquals("FAST", a.resources().get(0).resourceId());
        assertEquals(3, a.endTick() - a.startTick());
        assertEquals(3L, tier(result, 0).value());
    }

    // ------------------------------------------------------------------------------------ calendars

    @Test
    void aTaskLiesInsideOneWorkingWindowOfItsResource() {
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(), List.of(CpSatTestKit.calendar("day", 10, 15)),
                List.of(CpSatTestKit.resourceWithCalendar("R", "day")), List.of(task("T", 3)), options(10, 1, true), null);
        SolveResult result = solve(problem, List.of());
        assertOptimal(result);
        Assignment a = assignment(result, "T");
        assertTrue(a.startTick() >= 10 && a.endTick() <= 15, "task must lie in [10,15): " + a.startTick());
    }

    // ------------------------------------------------------------------------------------ slot mode

    @Test
    void slotModeStartsOnTheStartOfAnAllowedSlot() {
        PlanningProblem problem = CpSatTestKit.problem(40,
                List.of(CpSatTestKit.slot("s1", 2, 6), CpSatTestKit.slot("s2", 10, 14), CpSatTestKit.slot("s3", 20, 24)),
                List.of(), List.of(resource("R", 1)),
                List.of(CpSatTestKit.slotTask("T", 4, List.of("s2", "s3"))), options(10, 1, true), null);
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        Assignment a = assignment(result, "T");
        assertEquals(10, a.startTick(), "s1 is not allowed, s2 is the earliest allowed slot");
        assertEquals("s2", a.slotId());
        assertEquals(at(10), a.start());
        assertEquals(at(14), a.end());
    }

    @Test
    void aTaskIsNeverOfferedASlotShorterThanItsDuration() {
        PlanningProblem problem = CpSatTestKit.problem(40,
                List.of(CpSatTestKit.slot("short", 2, 4), CpSatTestKit.slot("long", 10, 20)),
                List.of(), List.of(resource("R", 1)),
                List.of(CpSatTestKit.slotTask("T", 5, List.of("short", "long"))), options(10, 1, true), null);
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        assertEquals("long", assignment(result, "T").slotId());
    }

    // ------------------------------------------------------------------------------------ precedence

    private SolveResult chain(PrecedenceRef.Type type, int minLag, Integer maxLag) {
        Task a = task("A", 3, true, null, List.of(on("R1")), List.of());
        Task b = task("B", 4, true, null, List.of(on("R2")),
                List.of(new PrecedenceRef("A", type, minLag, maxLag, null)));
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("R1", 1), resource("R2", 1)), List.of(a, b),
                options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        return result;
    }

    @Test
    void finishStartWithLag() {
        SolveResult result = chain(PrecedenceRef.Type.FINISH_START, 2, null);
        assertEquals(0, assignment(result, "A").startTick());
        assertEquals(5, assignment(result, "B").startTick(), "start(B) >= end(A) + 2");
    }

    @Test
    void startStartWithLag() {
        SolveResult result = chain(PrecedenceRef.Type.START_START, 2, null);
        assertEquals(2, assignment(result, "B").startTick() - assignment(result, "A").startTick());
    }

    @Test
    void finishFinishWithLag() {
        SolveResult result = chain(PrecedenceRef.Type.FINISH_FINISH, 1, null);
        assertTrue(assignment(result, "B").endTick() >= assignment(result, "A").endTick() + 1);
    }

    @Test
    void startFinishWithLag() {
        SolveResult result = chain(PrecedenceRef.Type.START_FINISH, 5, null);
        assertTrue(assignment(result, "B").endTick() >= assignment(result, "A").startTick() + 5);
        assertEquals(5L, tier(result, 0).value());
    }

    @Test
    void maxLagBoundsTheSuccessor() {
        Task a = task("A", 3, true, null, List.of(on("R1")), List.of());
        Task b = new Task("B", null, null, 4, null, null, null, true, null, List.of(on("R2")), null, null, null, null,
                new Pin(at(10), null), List.of(new PrecedenceRef("A", PrecedenceRef.Type.FINISH_START, 0, 2, null)), null,
                null, null, null);
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("R1", 1), resource("R2", 1)), List.of(a, b),
                options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        int endA = assignment(result, "A").endTick();
        assertEquals(10, assignment(result, "B").startTick());
        assertTrue(endA >= 8 && endA <= 10, "end(A) must be within [start(B) - 2, start(B)] but was " + endA);
    }

    @Test
    void impliesPresenceForcesTheOptionalPredecessor() {
        Task a = task("A", 3, false, 1L, List.of(on("R1")), List.of());
        Task b = task("B", 4, true, null, List.of(on("R2")), List.of(new PrecedenceRef("A",
                PrecedenceRef.Type.FINISH_START, 0, null, PrecedenceRef.Presence.IMPLIES)));
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("R1", 1), resource("R2", 1)), List.of(a, b),
                options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        assertNotNull(assignment(result, "A"), "B is required and implies A");
        assertTrue(result.solution().assignments().stream().anyMatch(x -> x.taskId().equals("A") && x.optional()));
    }

    // ------------------------------------------------------------------------------------ optional tasks

    @Test
    void prizeCollectingSelectsTheBestSubset() {
        List<Task> tasks = List.of(
                task("P1", 120, false, 50L, List.of(), List.of()),
                task("P2", 180, false, 80L, List.of(), List.of()),
                task("P3", 240, false, 90L, List.of(), List.of()),
                task("P4", 90, false, 30L, List.of(), List.of()),
                task("P5", 150, false, 60L, List.of(), List.of()));
        PlanningProblem problem = CpSatTestKit.problem(480, List.of(resource("tech", 1)), tasks, options(10, 1, true));
        SolveResult result = solve(problem, List.of(), VALUE);
        assertOptimal(result);
        assertEquals(-190L, tier(result, 0).value(), "reward tiers are negative internally");
        List<String> selected = CpSatTestKit.ids(result.solution().assignments());
        assertTrue(selected.containsAll(List.of("P1", "P2", "P5")) && selected.size() == 3, selected.toString());
        assertEquals(2, result.solution().unassignedTasks().size());
        assertEquals("NOT_SELECTED", result.solution().unassignedTasks().get(0).reasonCode());
        assertEquals(90L, result.solution().unassignedTasks().get(0).forgonePrize(), "P3 sorts before P4");
    }

    // ------------------------------------------------------------------------------------ explain / stats

    @Test
    void infeasibleResultCarriesTheViolatedConstraintInTheCore() {
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("R", 1)), List.of(task("T", 10)),
                options(10, 1, true));
        CompiledConstraint tooEarly = CpSatTestKit.latestEnd(0, "deadline", "T", 5,
                IEnumEnforcement.Types.STRICTLY_REQUIRED, 0);
        SolveResult result = solve(problem, List.of(tooEarly));
        assertEquals(IEnumSolverStatus.Types.INFEASIBLE, result.status());
        assertNull(result.solution());
        assertNotNull(result.diagnostics().core());
        assertTrue(result.diagnostics().infeasibilityCore().contains("deadline"),
                result.diagnostics().infeasibilityCore().toString());
    }

    @Test
    void explainBuildOmitsSoftConstraintsAndCreatesAssumptions() {
        Task a = task("A", 3, true, null, List.of(on("R1")), List.of());
        Task b = task("B", 4, true, null, List.of(on("R2")), List.of(new PrecedenceRef("A", null, 0, null, null)));
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("R1", 1), resource("R2", 1)), List.of(a, b),
                options(10, 1, true));
        CompiledConstraint hard = CpSatTestKit.latestEnd(0, "hard-end", "B", 30, IEnumEnforcement.Types.STRICTLY_REQUIRED,
                0);
        CompiledConstraint soft = CpSatTestKit.latestEnd(1, "soft-end", "B", 6, IEnumEnforcement.Types.SOFT, 0);
        CompiledProblem compiled = CpSatTestKit.compile(problem, List.of(hard, soft), List.of(MAKESPAN));
        BuiltModel built = CpSatTestKit.newBuilder(CpSatTestKit.constraints(), CpSatTestKit.objectives(),
                new CpSatProperties()).build(compiled, CpSatTestKit.context(problem.options(), null), BuildMode.EXPLAIN);
        List<String> ids = built.assumptions().stream().map(r -> r.kind() + ":" + r.id()).toList();
        assertTrue(ids.contains("CONSTRAINT:hard-end"), ids.toString());
        assertTrue(ids.contains("EDGE:A->B"), ids.toString());
        assertFalse(ids.contains("CONSTRAINT:soft-end"), "soft constraints are omitted in the explain pass");
        assertNull(built.plan(), "no objective in the explain pass");
        assertTrue(built.explainSkipped().isEmpty());
    }

    @Test
    void metricsDescribeTheModelAndTheSearch() {
        PlanningProblem problem = CpSatTestKit.problem(40, List.of(resource("M", 1)),
                List.of(task("a", 5), task("b", 5)), options(10, 1, true));
        SolveResult result = solve(problem, List.of(), MAKESPAN);
        assertOptimal(result);
        assertEquals(2, result.metrics().model().tasks());
        assertEquals(1, result.metrics().model().resources());
        assertEquals(2, result.metrics().model().candidates());
        assertTrue(result.metrics().model().intervals() >= 2);
        assertTrue(result.metrics().search().solutionsFound() >= 1);
        assertTrue(result.metrics().buildMillis() <= result.metrics().durationMillis());
        assertEquals(1, result.metrics().workersUsed());
        assertEquals(1.0d, result.solution().qualityScore());
        assertEquals(0, tier(result, 0).tier());
        assertEquals("OPTIMAL", tier(result, 0).status());
    }
}

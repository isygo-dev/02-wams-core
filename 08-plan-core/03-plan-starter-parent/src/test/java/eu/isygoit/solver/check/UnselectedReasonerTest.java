package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.enums.IEnumEnforcement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.check.TestProblems.assign;
import static eu.isygoit.solver.check.TestProblems.requirement;
import static eu.isygoit.solver.check.TestProblems.resource;
import static eu.isygoit.solver.check.TestProblems.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnselectedReasonerTest {

    private static List<UnassignedTask> reasons(PlanningProblem problem, StubIndexView index,
                                                eu.isygoit.dto.response.Assignment... selected) {
        SolutionViewImpl view = SolutionViewImpl.build(index, List.of(selected));
        return new UnselectedReasoner(problem, index).reason(view);
    }

    @Test
    void reasonsFollowTheDocumentedOrderAndAreSortedByTaskId() {
        PlanningProblem p = TestProblems.problem(100, List.of(resource("R", 1)), List.of(
                task("a-base", 10).build(),
                task("z-none", 5).optional(7L).requires(requirement("default", 1, null, "welding")).build(),
                task("y-window", 5).optional(3L).build(),
                task("x-pred", 5).optional(4L)
                        .after("a-base", PrecedenceRef.Type.FINISH_START, 0, null, PrecedenceRef.Presence.IMPLIES).build(),
                task("w-other", 5).optional(null).build()), List.of(), List.of());
        StubIndexView index = StubIndexView.of(p).window("y-window", 50, 40);
        // a-base is NOT selected, so the IMPLIES successor has no predecessor either
        List<UnassignedTask> out = reasons(p, index);
        assertEquals(List.of("w-other", "x-pred", "y-window", "z-none"),
                out.stream().map(UnassignedTask::taskId).toList());
        Map<String, UnassignedTask> byId = new java.util.HashMap<>();
        out.forEach(u -> byId.put(u.taskId(), u));
        assertEquals(UnselectedReasoner.NOT_SELECTED, byId.get("w-other").reasonCode());
        assertEquals(1L, byId.get("w-other").forgonePrize(), "prize defaults to 1");
        assertEquals(UnselectedReasoner.PREDECESSOR_NOT_SELECTED, byId.get("x-pred").reasonCode());
        assertEquals(4L, byId.get("x-pred").forgonePrize());
        assertEquals(UnselectedReasoner.OUTSIDE_WINDOW, byId.get("y-window").reasonCode());
        assertEquals(UnselectedReasoner.NO_ELIGIBLE_RESOURCE, byId.get("z-none").reasonCode());
        assertEquals(7L, byId.get("z-none").forgonePrize());
    }

    @Test
    void selectedAndRequiredTasksAreNeverListed() {
        PlanningProblem p = TestProblems.problem(100, List.of(resource("R", 1)), List.of(
                task("req", 10).build(), task("picked", 5).optional(2L).build(), task("left", 5).optional(2L).build()),
                List.of(), List.of());
        StubIndexView index = StubIndexView.of(p);
        List<UnassignedTask> out = reasons(p, index, assign("req", 0, 10, "R"),
                assign("picked", 10, 15, 1, true, "R"));
        assertEquals(1, out.size());
        assertEquals("left", out.get(0).taskId());
    }

    @Test
    void impliesPredecessorThatIsSelectedDoesNotExplainTheAbsence() {
        PlanningProblem p = TestProblems.problem(100, List.of(resource("R", 1)), List.of(
                task("base", 10).build(),
                task("succ", 5).optional(2L)
                        .after("base", PrecedenceRef.Type.FINISH_START, 0, null, PrecedenceRef.Presence.IMPLIES).build()),
                List.of(), List.of());
        List<UnassignedTask> out = reasons(p, StubIndexView.of(p), assign("base", 0, 10, "R"));
        assertEquals(UnselectedReasoner.NOT_SELECTED, out.get(0).reasonCode());
    }

    @Test
    void noEligibleResourceSuggestionNamesThePruningConstraints() {
        Constraint eligibility = new Constraint("elig-1", "task_eligibility",
                IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("task", "lonely"), Map.of(), null, 0, null);
        Constraint soft = new Constraint("elig-soft", "task_eligibility",
                IEnumEnforcement.Types.SOFT, new ConstraintScope("task", "lonely"), Map.of(), null, 1, null);
        PlanningProblem p = TestProblems.problem(100, List.of(resource("R", 1)),
                List.of(task("lonely", 5).optional(1L).build()), List.of(eligibility, soft), List.of());
        StubIndexView index = StubIndexView.of(p).prune("lonely", "R");
        UnassignedTask out = reasons(p, index).get(0);
        assertEquals(UnselectedReasoner.NO_ELIGIBLE_RESOURCE, out.reasonCode());
        assertTrue(out.suggestion().contains("elig-1"));
        assertTrue(!out.suggestion().contains("elig-soft"));
    }
}

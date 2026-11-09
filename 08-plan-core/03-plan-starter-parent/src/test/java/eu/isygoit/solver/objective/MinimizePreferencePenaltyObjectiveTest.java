package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Preference;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import eu.isygoit.solver.objective.ObjectiveTestSupport.RecordingValidation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.at;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.ctx;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.objective;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.problem;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.resource;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimizePreferencePenaltyObjectiveTest {

    private final MinimizePreferencePenaltyObjective family = new MinimizePreferencePenaltyObjective();

    private PlanningProblem scenario() {
        return problem(List.of(
                task("t1").preference(new Preference(Preference.Kind.RESOURCE, "alice", null, 5))
                        .preference(new Preference(Preference.Kind.START, null, at(10), 2)).build(),
                task("t2").optional().preference(new Preference(Preference.Kind.RESOURCE, "alice", null, 7)).build(),
                task("t3").preference(new Preference(Preference.Kind.RESOURCE, "ghost", null, 3)).build()),
                List.of(resource("alice"), resource("bob")));
    }

    @Test
    void identity() {
        assertEquals("minimize_preference_penalty", family.type());
        assertFalse(family.reward());
    }

    @Test
    void resourcePenaltyOnlyWhenNotOnThePreferredResource() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        FakeSolution onAlice = new FakeSolution(3, 2).place(0, 10, 20, 0).place(2, 0, 5, 0);
        // t1 on alice and starting at its preferred tick: 0; t3 prefers an unknown resource: 3
        assertEquals(3, family.evaluator().evaluate(ctx(p, index, onAlice), objective("minimize_preference_penalty", 1), null));
        FakeSolution onBob = new FakeSolution(3, 2).place(0, 10, 20, 1).place(2, 0, 5, 0);
        assertEquals(8, family.evaluator().evaluate(ctx(p, index, onBob), objective("minimize_preference_penalty", 1), null));
    }

    @Test
    void startPenaltyIsPenaltyTimesTickDeviation() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(3, 2).place(0, 14, 24, 0).place(2, 0, 5, 0);
        // t1: start deviation |14-10| * 2 = 8, t3: 3
        assertEquals(11, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_preference_penalty", 1), null));
    }

    @Test
    void unselectedOptionalTaskHasNoPenaltyButSelectedOneOffResourceDoes() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(3, 2).place(0, 10, 20, 0).place(1, 0, 5, 1).place(2, 0, 5, 0);
        assertEquals(3 + 7, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_preference_penalty", 1), null));
    }

    @Test
    void validateReportsUnknownResourceAndNegativePenalty() {
        PlanningProblem p = problem(List.of(
                task("t").preference(new Preference(Preference.Kind.RESOURCE, "ghost", null, -1)).build()),
                List.of(resource("alice")));
        RecordingValidation v = new RecordingValidation(p);
        family.validate(v, new Objective("obj", "minimize_preference_penalty", 1), null);
        assertTrue(v.errors.contains("UNKNOWN_REFERENCE@tasks[0].preferences[0].resourceId"), v.errors.toString());
        assertTrue(v.errors.contains("OBJECTIVE_PARAMS_INVALID@tasks[0].preferences[0].penalty"), v.errors.toString());
    }
}

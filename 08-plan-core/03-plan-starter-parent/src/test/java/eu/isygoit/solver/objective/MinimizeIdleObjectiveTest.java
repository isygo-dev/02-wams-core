package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import eu.isygoit.solver.objective.ObjectiveTestSupport.RecordingValidation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.ctx;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.objective;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.problem;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.resource;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimizeIdleObjectiveTest {

    private final MinimizeIdleObjective family = new MinimizeIdleObjective();

    private PlanningProblem scenario() {
        return problem(List.of(task("a").duration(10).build(), task("b").duration(10).build(),
                task("c").duration(5).build()), List.of(resource("m1"), resource("m2")));
    }

    @Test
    void identity() {
        assertEquals("minimize_idle", family.type());
        assertFalse(family.reward());
    }

    @Test
    void idleIsSpanMinusLoadPerUsedResource() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        // m1: [0,10) and [25,35): span 35, load 20, idle 15; m2: [3,8): span 5, load 5, idle 0
        FakeSolution s = new FakeSolution(3, 2).place(0, 0, 10, 0).place(1, 25, 35, 0).place(2, 3, 8, 1);
        assertEquals(15, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_idle", 1), null));
    }

    @Test
    void unusedResourceAndSingleTaskHaveNoIdle() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(3, 2).place(0, 4, 14, 0);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_idle", 1), null));
    }

    @Test
    void cumulativeResourcesAreSkippedAndReportedByValidation() {
        PlanningProblem p = problem(List.of(task("a").duration(10).build(), task("b").duration(10).build()),
                List.of(resource("pool", 2, 0.0, 0.0, null)));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}});
        FakeSolution s = new FakeSolution(2, 1).place(0, 0, 10, 0).place(1, 0, 10, 0);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_idle", 1), null));

        RecordingValidation v = new RecordingValidation(p);
        family.validate(v, new Objective("obj", "minimize_idle", 1), null);
        assertTrue(v.warnings.get(0).startsWith("OBJECTIVE_IDLE_CUMULATIVE_IGNORED@resources[0].capacity"),
                v.warnings.toString());
    }
}

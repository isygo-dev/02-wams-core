package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.ctx;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.objective;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.problem;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.resource;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaximizeTaskPriorityObjectiveTest {

    private final MaximizeTaskPriorityObjective family = new MaximizeTaskPriorityObjective();

    /** G3 shape: one required task (priority 100, never counted) and optional tasks with priority 9 / 5 / 1. */
    private PlanningProblem scenario() {
        return problem(List.of(task("mandatory").priority(100).build(), task("high").priority(9).optional().build(),
                task("medium").priority(5).optional().build(), task("low").priority(1).optional().build()),
                List.of(resource("lab")));
    }

    @Test
    void identityIsAReward() {
        assertEquals("maximize_task_priority", family.type());
        assertTrue(family.reward());
    }

    @Test
    void sumsPriorityOfSelectedOptionalTasksOnly() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}, {0}});
        FakeSolution s = new FakeSolution(4, 1).place(0, 0, 10, 0).place(1, 10, 20, 0);
        assertEquals(9, family.evaluator().evaluate(ctx(p, index, s), objective("maximize_task_priority", 1), null));
        s.place(3, 20, 30, 0);
        assertEquals(10, family.evaluator().evaluate(ctx(p, index, s), objective("maximize_task_priority", 1), null));
    }

    @Test
    void noOptionalSelectedIsZero() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}, {0}});
        FakeSolution s = new FakeSolution(4, 1).place(0, 0, 10, 0);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("maximize_task_priority", 1), null));
    }
}

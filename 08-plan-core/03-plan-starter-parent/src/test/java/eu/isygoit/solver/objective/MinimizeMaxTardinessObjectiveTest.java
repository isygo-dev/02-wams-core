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
import static org.junit.jupiter.api.Assertions.assertFalse;

class MinimizeMaxTardinessObjectiveTest {

    private final MinimizeMaxTardinessObjective family = new MinimizeMaxTardinessObjective();

    private PlanningProblem scenario() {
        return problem(List.of(
                task("a").duration(10).dueTick(8).build(),
                task("b").duration(10).dueTick(15).build(),
                task("c").duration(10).dueTick(5).optional().build()), List.of(resource("m")));
    }

    @Test
    void identity() {
        assertEquals("minimize_max_tardiness", family.type());
        assertEquals(Void.class, family.paramsType());
        assertFalse(family.reward());
    }

    @Test
    void takesTheLargestLateness() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 10, 0).place(1, 10, 20, 0);
        assertEquals(5, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_max_tardiness", 1), null));
    }

    @Test
    void selectedOptionalTaskCountsUnselectedDoesNot() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 10, 0).place(1, 10, 20, 0).place(2, 20, 30, 0);
        assertEquals(25, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_max_tardiness", 1), null));
    }

    @Test
    void nothingLateIsZero() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 8, 0);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_max_tardiness", 1), null));
    }
}

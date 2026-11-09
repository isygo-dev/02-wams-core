package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.exception.ObjectiveRangeException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaximizeSelectedValueObjectiveTest {

    private final MaximizeSelectedValueObjective family = new MaximizeSelectedValueObjective();

    /** G11 shape: five optional tasks with prizes 50, 80, 90, 30, 60. */
    private PlanningProblem g11() {
        return problem(List.of(task("P1").optional().prize(50).build(), task("P2").optional().prize(80).build(),
                task("P3").optional().prize(90).build(), task("P4").optional().prize(30).build(),
                task("P5").optional().prize(60).build()), List.of(resource("tech")));
    }

    @Test
    void identityIsAReward() {
        assertEquals("maximize_selected_value", family.type());
        assertTrue(family.reward());
    }

    @Test
    void g11SelectedP1P2P5IsOneHundredNinety() {
        PlanningProblem p = g11();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}, {0}, {0}});
        FakeSolution s = new FakeSolution(5, 1).place(0, 0, 10, 0).place(1, 10, 20, 0).place(4, 20, 30, 0);
        assertEquals(190, family.evaluator().evaluate(ctx(p, index, s), objective("maximize_selected_value", 1), null));
    }

    @Test
    void missingPrizeDefaultsToOneAndRequiredTasksAreIgnored() {
        PlanningProblem p = problem(List.of(task("a").optional().build(), task("b").build(),
                task("c").optional().prize(0).build()), List.of(resource("r")));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 10, 0).place(1, 10, 20, 0).place(2, 20, 30, 0);
        assertEquals(1, family.evaluator().evaluate(ctx(p, index, s), objective("maximize_selected_value", 1), null));
    }

    @Test
    void totalAbovePowerOfTwoFiftyThreeIsRejected() {
        long big = (1L << 53);
        PlanningProblem p = problem(List.of(task("a").optional().prize(big).build(), task("b").optional().prize(big).build()),
                List.of(resource("r")));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}});
        FakeSolution s = new FakeSolution(2, 1).place(0, 0, 10, 0).place(1, 10, 20, 0);
        assertThrows(ObjectiveRangeException.class,
                () -> family.evaluator().evaluate(ctx(p, index, s), objective("maximize_selected_value", 1), null));
    }
}

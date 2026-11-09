package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.TardinessParams;
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

class MinimizeTotalTardinessObjectiveTest {

    private final MinimizeTotalTardinessObjective family = new MinimizeTotalTardinessObjective();

    private PlanningProblem scenario() {
        return problem(List.of(
                task("a").duration(10).dueTick(8).priority(0).build(),
                task("b").duration(10).dueTick(15).priority(3).build(),
                task("c").duration(10).priority(9).build()), List.of(resource("m")));
    }

    private FakeSolution solution() {
        return new FakeSolution(3, 1).place(0, 0, 10, 0).place(1, 10, 20, 0).place(2, 20, 30, 0);
    }

    @Test
    void identity() {
        assertEquals("minimize_total_tardiness", family.type());
        assertEquals(TardinessParams.class, family.paramsType());
        assertFalse(family.reward());
        assertEquals(List.of("weightBy"), family.paramsHint());
    }

    @Test
    void sumsLatenessOfTasksWithDueDateOnly() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        // a: 10-8 = 2, b: 20-15 = 5, c has no due date
        assertEquals(7, family.evaluator().evaluate(ctx(p, index, solution()), objective("minimize_total_tardiness", 1),
                new TardinessParams(null)));
        assertEquals(7, family.evaluator().evaluate(ctx(p, index, solution()), objective("minimize_total_tardiness", 1), null));
    }

    @Test
    void priorityWeightingMultipliesByOnePlusPriority() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        // a: 2 * (1+0) + b: 5 * (1+3)
        assertEquals(22, family.evaluator().evaluate(ctx(p, index, solution()), objective("minimize_total_tardiness", 1),
                new TardinessParams(TardinessParams.WeightBy.PRIORITY)));
    }

    @Test
    void earlyTasksAndUnselectedTasksCostNothing() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 8, 0);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_total_tardiness", 1), null));
    }

    @Test
    void warnsWhenNoTaskHasADueDate() {
        PlanningProblem p = problem(List.of(task("x").build()), List.of(resource("m")));
        RecordingValidation v = new RecordingValidation(p);
        family.validate(v, new Objective("obj", "minimize_total_tardiness", 1), null);
        assertTrue(v.warnings.get(0).startsWith("OBJECTIVE_NO_EFFECT"), v.warnings.toString());
    }
}

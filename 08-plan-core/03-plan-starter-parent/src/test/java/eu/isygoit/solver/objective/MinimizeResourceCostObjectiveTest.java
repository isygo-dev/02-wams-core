package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.exception.ObjectiveRangeException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimizeResourceCostObjectiveTest {

    private final MinimizeResourceCostObjective family = new MinimizeResourceCostObjective();

    /** G6 shape: three 60-tick jobs, cheap (1.0/tick) and expensive (3.0/tick) resources. */
    private PlanningProblem g6() {
        return problem(List.of(task("j1").duration(60).build(), task("j2").duration(60).build(),
                task("j3").duration(60).build()),
                List.of(resource("cheap", 1, 1.0, 0.0, null), resource("expensive", 1, 3.0, 0.0, null)));
    }

    private FakeSolution g6Solution() {
        return new FakeSolution(3, 2).place(0, 0, 60, 0).place(1, 60, 120, 0).place(2, 0, 60, 1);
    }

    @Test
    void identity() {
        assertEquals("minimize_resource_cost", family.type());
        assertFalse(family.reward());
    }

    @Test
    void g6ValueIsThirtyThousandAtDefaultScale() {
        PlanningProblem p = g6();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        // 2 * 60 * (1.0*100) + 1 * 60 * (3.0*100) = 12000 + 18000
        assertEquals(30000, family.evaluator().evaluate(ctx(p, index, g6Solution()),
                objective("minimize_resource_cost", 1), null));
    }

    @Test
    void scaleOneThousandMultipliesTenfold() {
        PlanningProblem p = g6();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        Objective o = objective("minimize_resource_cost", ConstraintScope.all(), 1000);
        assertEquals(300000, family.evaluator().evaluate(ctx(p, index, g6Solution()), o, null));
    }

    @Test
    void decimalCostsRoundHalfUp() {
        PlanningProblem p = problem(List.of(task("t").duration(10).build()),
                List.of(resource("r", 1, 2.675, 0.0, null)));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}});
        FakeSolution s = new FakeSolution(1, 1).place(0, 0, 10, 0);
        // scale(2.675, 100) = 268 (half-up on the decimal value), 10 ticks
        assertEquals(2680, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_resource_cost", 1), null));
    }

    @Test
    void costPerUseIsPaidOncePerUsedResource() {
        PlanningProblem p = problem(List.of(task("t1").duration(10).build(), task("t2").duration(10).build()),
                List.of(resource("r", 1, 0.0, 5.0, null), resource("unused", 1, 0.0, 9.0, null)));
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(2, 2).place(0, 0, 10, 0).place(1, 10, 20, 0);
        assertEquals(500, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_resource_cost", 1), null));
    }

    @Test
    void scopeRestrictsResources() {
        PlanningProblem p = g6();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        Objective onlyCheap = objective("minimize_resource_cost", new ConstraintScope("resource", "cheap"), 100);
        assertEquals(12000, family.evaluator().evaluate(ctx(p, index, g6Solution()), onlyCheap, null));
    }

    @Test
    void overflowRaisesObjectiveRangeException() {
        PlanningProblem p = problem(List.of(task("t").duration(10).build()),
                List.of(resource("r", 1, 1.0e14, 0.0, null)));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}});
        FakeSolution s = new FakeSolution(1, 1).place(0, 0, 10, 0);
        assertThrows(ObjectiveRangeException.class,
                () -> family.evaluator().evaluate(ctx(p, index, s), objective("minimize_resource_cost", 1), null));
    }

    @Test
    void validateReportsRangeErrorsAndNoEffect() {
        PlanningProblem huge = problem(List.of(task("t").build()), List.of(resource("r", 1, 1.0e14, 0.0, null)));
        RecordingValidation v = new RecordingValidation(huge);
        family.validate(v, new Objective("obj", "minimize_resource_cost", 1), null);
        assertTrue(v.errors.get(0).startsWith("OBJECTIVE_SCORE_OUT_OF_RANGE@resources[0].costPerTick"), v.errors.toString());

        PlanningProblem free = problem(List.of(task("t").build()), List.of(resource("r")));
        RecordingValidation w = new RecordingValidation(free);
        family.validate(w, new Objective("obj", "minimize_resource_cost", 1), null);
        assertTrue(w.warnings.get(0).startsWith("OBJECTIVE_NO_EFFECT"), w.warnings.toString());
    }
}

package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import eu.isygoit.solver.objective.ObjectiveTestSupport.RecordingValidation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.ctx;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.initial;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.objective;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.problem;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.resource;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.slot;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimizeSlotCostObjectiveTest {

    private final MinimizeSlotCostObjective family = new MinimizeSlotCostObjective();

    /** G1 shape: slot-early (cost 0), slot-late (cost 1.0), three one-hour sessions. */
    private PlanningProblem scenario() {
        return problem(List.of(task("s1").tag("x").build(), task("s2").build(), task("s3").build()),
                List.of(resource("lab", 2, 0.0, 0.0, null)),
                List.of(slot("slot-early", 0, 60, 0.0), slot("slot-late", 60, 120, 1.0)),
                List.of(), initial(), List.of());
    }

    @Test
    void identity() {
        assertEquals("minimize_slot_cost", family.type());
        assertFalse(family.reward());
    }

    @Test
    void g1ValueIsOneHundredAtDefaultScale() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1)
                .place(0, 0, 60, 0).slot(0, 0)
                .place(1, 0, 60, 0).slot(1, 0)
                .place(2, 60, 120, 0).slot(2, 1);
        assertEquals(100, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_slot_cost", 1), null));
    }

    @Test
    void decimalCostFollowsTheObjectiveScale() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(2, 60, 120, 0).slot(2, 1);
        Objective scale1000 = objective("minimize_slot_cost", ConstraintScope.all(), 1000);
        assertEquals(1000, family.evaluator().evaluate(ctx(p, index, s), scale1000, null));
    }

    @Test
    void tasksWithoutSlotOrUnselectedCostNothing() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 70, 80, 0).slot(1, 1);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_slot_cost", 1), null));
    }

    @Test
    void validateRejectsNegativeSlotCost() {
        PlanningProblem p = problem(List.of(task("t").build()), List.of(resource("r")),
                List.of(slot("neg", 0, 10, -1.0)), List.of(), initial(), List.of());
        RecordingValidation v = new RecordingValidation(p);
        family.validate(v, new Objective("obj", "minimize_slot_cost", 1), null);
        assertTrue(v.errors.get(0).startsWith("OBJECTIVE_SCORE_OUT_OF_RANGE@slots[0].cost"), v.errors.toString());
    }
}

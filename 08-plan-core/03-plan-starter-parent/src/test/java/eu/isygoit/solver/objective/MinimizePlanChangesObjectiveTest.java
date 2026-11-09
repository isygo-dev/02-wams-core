package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.PlanChangeParams;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import eu.isygoit.solver.objective.ObjectiveTestSupport.RecordingValidation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.at;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.ctx;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.initial;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.objective;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.problem;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.resource;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimizePlanChangesObjectiveTest {

    private final MinimizePlanChangesObjective family = new MinimizePlanChangesObjective();

    private PlanningProblem scenario() {
        return problem(List.of(task("a").build(), task("b").build(), task("c").optional().build()),
                List.of(resource("m1"), resource("m2")), List.of(), List.of(),
                initial(new InitialAssignment("a", at(0), List.of("m1"), false),
                        new InitialAssignment("b", at(20), List.of("m1"), true),
                        new InitialAssignment("c", at(40), List.of("m2"), false)),
                List.of());
    }

    @Test
    void identity() {
        assertEquals("minimize_plan_changes", family.type());
        assertEquals(PlanChangeParams.class, family.paramsType());
        assertFalse(family.reward());
    }

    @Test
    void unchangedPlanCostsNothing() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(3, 2).place(0, 0, 10, 0).place(1, 20, 30, 0).place(2, 40, 50, 1);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_plan_changes", 1), null));
    }

    @Test
    void startMovesAndResourceChangesAreWeighted() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        // a moved 7 ticks (weight 2) and changed resource (weight 20); b is locked (skipped even if it differs);
        // c is unselected (no deviation)
        FakeSolution s = new FakeSolution(3, 2).place(0, 7, 17, 1).place(1, 99, 109, 1);
        assertEquals(7 * 2 + 20, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_plan_changes", 1),
                new PlanChangeParams(2, 20)));
        // defaults: weights 1 and 1
        assertEquals(7 + 1, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_plan_changes", 1), null));
    }

    @Test
    void unknownInitialResourceAlwaysCountsOnceSelected() {
        PlanningProblem p = problem(List.of(task("a").build()), List.of(resource("m1")), List.of(), List.of(),
                initial(new InitialAssignment("a", null, List.of("gone"), false)), List.of());
        FakeIndex index = new FakeIndex(p, new int[][]{{0}});
        FakeSolution s = new FakeSolution(1, 1).place(0, 3, 13, 0);
        assertEquals(1, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_plan_changes", 1), null));
    }

    @Test
    void validateRejectsNegativeWeightsAndWarnsWithoutHints() {
        PlanningProblem p = scenario();
        RecordingValidation v = new RecordingValidation(p);
        family.validate(v, new Objective("obj", "minimize_plan_changes", 1, 0, null, null, Map.of()),
                new PlanChangeParams(-1, 1));
        assertTrue(v.errors.get(0).startsWith("OBJECTIVE_PARAMS_INVALID"), v.errors.toString());

        PlanningProblem none = problem(List.of(task("a").build()), List.of(resource("m1")));
        RecordingValidation w = new RecordingValidation(none);
        family.validate(w, new Objective("obj", "minimize_plan_changes", 1), null);
        assertTrue(w.warnings.get(0).startsWith("OBJECTIVE_NO_EFFECT"), w.warnings.toString());
    }
}

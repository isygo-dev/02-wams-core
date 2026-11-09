package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import eu.isygoit.solver.objective.ObjectiveTestSupport.RecordingValidation;
import eu.isygoit.solver.spi.ModelCost;
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

class MinimizeMakespanObjectiveTest {

    private final MinimizeMakespanObjective family = new MinimizeMakespanObjective();

    private PlanningProblem scenario() {
        return problem(List.of(task("a").duration(3).tag("g1").build(), task("b").duration(4).tag("g2").build(),
                task("c").duration(5).tag("g1").optional().build()), List.of(resource("m")));
    }

    @Test
    void identity() {
        assertEquals("minimize_makespan", family.type());
        assertEquals(Void.class, family.paramsType());
        assertFalse(family.reward());
        assertEquals("plan.objective.minimize_makespan.description", family.descriptionKey());
        assertEquals(ModelCost.ZERO.boolVars(), family.estimate(new FakeIndex(scenario(), new int[][]{{0}, {0}, {0}}),
                objective("minimize_makespan", 1), null).boolVars());
    }

    @Test
    void maxEndOfSelectedTasks() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 3, 0).place(1, 3, 7, 0).place(2, 7, 12, 0);
        assertEquals(12, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_makespan", 1), null));
    }

    @Test
    void unselectedOptionalTaskIsIgnoredAndEmptyIsZero() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 3, 0).place(1, 3, 7, 0);
        assertEquals(7, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_makespan", 1), null));
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, new FakeSolution(3, 1)),
                objective("minimize_makespan", 1), null));
    }

    @Test
    void scopeRestrictsTasks() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 3, 0).place(1, 3, 7, 0).place(2, 7, 12, 0);
        Objective g1 = objective("minimize_makespan", new ConstraintScope("tag", "g1"), 100);
        assertEquals(12, family.evaluator().evaluate(ctx(p, index, s), g1, null));
        Objective g2 = objective("minimize_makespan", new ConstraintScope("tag", "g2"), 100);
        assertEquals(7, family.evaluator().evaluate(ctx(p, index, s), g2, null));
    }

    @Test
    void validateRejectsResourceScopeAndWarnsOnZeroWeight() {
        PlanningProblem p = scenario();
        RecordingValidation v = new RecordingValidation(p);
        family.validate(v, new Objective("obj", "minimize_makespan", 1, 0, null, new ConstraintScope("resource", "m"),
                java.util.Map.of()), null);
        assertTrue(v.errors.get(0).startsWith("INVALID_SCOPE@objectives[0]"), v.errors.toString());

        RecordingValidation w = new RecordingValidation(p);
        family.validate(w, objective("minimize_makespan", 0), null);
        assertTrue(w.warnings.get(0).startsWith("OBJECTIVE_WEIGHT_ZERO"), w.warnings.toString());
        assertTrue(w.errors.isEmpty());
    }

}

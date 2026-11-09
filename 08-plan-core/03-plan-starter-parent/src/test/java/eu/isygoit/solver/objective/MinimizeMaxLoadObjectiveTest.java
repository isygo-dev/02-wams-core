package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.LoadObjectiveParams;
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

class MinimizeMaxLoadObjectiveTest {

    private final MinimizeMaxLoadObjective family = new MinimizeMaxLoadObjective();

    /** G9 shape: five 8-tick shifts over three nurses. */
    private PlanningProblem scenario() {
        return problem(List.of(task("d0").duration(8).build(), task("d1").duration(8).build(),
                task("d2").duration(8).build(), task("d3").duration(8).build(), task("d4").duration(8).build()),
                List.of(resource("ana"), resource("ben"), resource("cleo")));
    }

    private FakeSolution solution() {
        return new FakeSolution(5, 3).place(0, 0, 8, 0).place(1, 20, 28, 0).place(2, 12, 20, 1).place(3, 40, 48, 1)
                .place(4, 30, 38, 2);
    }

    @Test
    void identity() {
        assertEquals("minimize_max_load", family.type());
        assertEquals(LoadObjectiveParams.class, family.paramsType());
        assertFalse(family.reward());
    }

    @Test
    void g9MaxLoadIsSixteenTicks() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1, 2}, {0, 1, 2}, {0, 1, 2}, {0, 1, 2}, {0, 1, 2}});
        assertEquals(16, family.evaluator().evaluate(ctx(p, index, solution()), objective("minimize_max_load", 1),
                new LoadObjectiveParams(LoadMetric.TICKS)));
        assertEquals(16, family.evaluator().evaluate(ctx(p, index, solution()), objective("minimize_max_load", 1), null));
    }

    @Test
    void countMetricCountsTasks() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1, 2}, {0, 1, 2}, {0, 1, 2}, {0, 1, 2}, {0, 1, 2}});
        assertEquals(2, family.evaluator().evaluate(ctx(p, index, solution()), objective("minimize_max_load", 1),
                new LoadObjectiveParams(LoadMetric.COUNT)));
    }

    @Test
    void scopeAndCandidatelessResources() {
        PlanningProblem p = scenario();
        // resource 2 (cleo) has no candidate at all
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}, {0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(5, 3).place(0, 0, 8, 0).place(4, 30, 38, 2);
        assertEquals(8, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_max_load", 1), null));
        Objective ben = objective("minimize_max_load", new ConstraintScope("resource", "ben"), 100);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), ben, null));
    }
}

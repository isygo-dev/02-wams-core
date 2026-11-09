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

class BalanceLoadObjectiveTest {

    private final BalanceLoadObjective family = new BalanceLoadObjective();

    private PlanningProblem scenario() {
        return problem(List.of(task("a").duration(10).build(), task("b").duration(6).build(),
                task("c").duration(4).build()), List.of(resource("r1"), resource("r2"), resource("r3")));
    }

    @Test
    void identity() {
        assertEquals("balance_load", family.type());
        assertEquals(LoadObjectiveParams.class, family.paramsType());
        assertFalse(family.reward());
    }

    @Test
    void spreadIsMaxMinusMinOverResourcesWithCandidates() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1, 2}, {0, 1, 2}, {0, 1, 2}});
        FakeSolution s = new FakeSolution(3, 3).place(0, 0, 10, 0).place(1, 0, 6, 1).place(2, 0, 4, 1);
        // loads: r1 = 10, r2 = 10, r3 = 0
        assertEquals(10, family.evaluator().evaluate(ctx(p, index, s), objective("balance_load", 1),
                new LoadObjectiveParams(LoadMetric.TICKS)));
        // counts: 1, 2, 0
        assertEquals(2, family.evaluator().evaluate(ctx(p, index, s), objective("balance_load", 1),
                new LoadObjectiveParams(LoadMetric.COUNT)));
    }

    @Test
    void resourcesWithoutCandidatesDoNotPullTheMinimumDown() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1}, {0, 1}, {0, 1}});
        FakeSolution s = new FakeSolution(3, 3).place(0, 0, 10, 0).place(1, 0, 6, 1).place(2, 6, 10, 1);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("balance_load", 1), null));
    }

    @Test
    void fewerThanTwoResourcesGivesZero() {
        PlanningProblem p = scenario();
        FakeIndex index = new FakeIndex(p, new int[][]{{0, 1, 2}, {0, 1, 2}, {0, 1, 2}});
        FakeSolution s = new FakeSolution(3, 3).place(0, 0, 10, 0);
        Objective single = objective("balance_load", new ConstraintScope("resource", "r1"), 100);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), single, null));
    }
}

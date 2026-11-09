package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.SetupEntry;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeIndex;
import eu.isygoit.solver.objective.ObjectiveTestSupport.FakeSolution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.ctx;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.initial;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.objective;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.problem;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.resource;
import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MinimizeSetupObjectiveTest {

    private final MinimizeSetupObjective family = new MinimizeSetupObjective();

    private PlanningProblem g14(SetupMatrix matrix) {
        return problem(List.of(task("A1").duration(10).setupClass("A").build(), task("A2").duration(10).setupClass("A").build(),
                        task("B1").duration(10).setupClass("B").build()),
                List.of(resource("press", 1, 0.0, 0.0, "paint")), List.of(), List.of(matrix), initial(), List.of());
    }

    @Test
    void identity() {
        assertEquals("minimize_setup", family.type());
        assertFalse(family.reward());
    }

    @Test
    void g14AdjacentSameClassTasksCostOneSwitchOfFiveTicks() {
        PlanningProblem p = g14(new SetupMatrix("paint", 5, 0, List.of()));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        // A1 [0,10) A2 [10,20) B1 [25,35): only A2 -> B1 pays 5
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 10, 0).place(1, 10, 20, 0).place(2, 25, 35, 0);
        assertEquals(5, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_setup", 1), null));
    }

    @Test
    void interleavedClassesPayTwice() {
        PlanningProblem p = g14(new SetupMatrix("paint", 5, 0, List.of()));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 10, 0).place(2, 15, 25, 0).place(1, 30, 40, 0);
        assertEquals(10, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_setup", 1), null));
    }

    @Test
    void explicitEntriesAndSameClassTicksOverrideTheDefault() {
        PlanningProblem p = g14(new SetupMatrix("paint", 5, 2, List.of(new SetupEntry("A", "B", 9))));
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}, {0}});
        // A1 -> A2 = sameClassTicks 2, A2 -> B1 = entry 9
        FakeSolution s = new FakeSolution(3, 1).place(0, 0, 10, 0).place(1, 12, 22, 0).place(2, 31, 41, 0);
        assertEquals(11, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_setup", 1), null));
    }

    @Test
    void firstTaskAndTasksWithoutClassHaveNoSetup() {
        PlanningProblem p = problem(List.of(task("x").build(), task("y").setupClass("B").build()),
                List.of(resource("press", 1, 0.0, 0.0, "paint")), List.of(),
                List.of(new SetupMatrix("paint", 5, 0, List.of())), initial(), List.of());
        FakeIndex index = new FakeIndex(p, new int[][]{{0}, {0}});
        FakeSolution s = new FakeSolution(2, 1).place(0, 0, 10, 0).place(1, 20, 30, 0);
        assertEquals(0, family.evaluator().evaluate(ctx(p, index, s), objective("minimize_setup", 1), null));
    }
}

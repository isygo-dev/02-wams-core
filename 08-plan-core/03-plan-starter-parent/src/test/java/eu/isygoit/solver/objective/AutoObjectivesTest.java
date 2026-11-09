package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.objective.ObjectiveTestSupport.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoObjectivesTest {

    private final Task required = task("r").build();
    private final Task optional = task("o").optional().prize(5).build();

    @Test
    void injectsSelectedValueAtTierMinusOneWhenOptionalTasksAndNoSelectionObjective() {
        List<Objective> result = AutoObjectives.inject(List.of(required, optional), List.of());
        assertEquals(1, result.size());
        Objective auto = result.get(0);
        assertEquals("auto-selected-value", auto.id());
        assertEquals("maximize_selected_value", auto.type());
        assertEquals(1, auto.weight());
        assertEquals(-1, auto.tier());
        assertEquals("all", auto.scope().kind());
        assertTrue(auto.params().isEmpty());
    }

    @Test
    void appendsAfterTheDeclaredObjectivesKeepingTheirOrder() {
        Objective makespan = new Objective("makespan", "minimize_makespan", 1);
        List<Objective> result = AutoObjectives.inject(List.of(optional), List.of(makespan));
        assertEquals(2, result.size());
        assertSame(makespan, result.get(0));
        assertEquals("auto-selected-value", result.get(1).id());
    }

    @Test
    void doesNothingWithoutOptionalTasks() {
        List<Objective> declared = List.of(new Objective("makespan", "minimize_makespan", 1));
        assertEquals(declared, AutoObjectives.inject(List.of(required), declared));
        assertTrue(AutoObjectives.inject(List.of(required), List.of()).isEmpty());
    }

    @Test
    void declaringEitherSelectionObjectiveDisablesTheInjection() {
        List<Objective> value = List.of(new Objective("v", "maximize_selected_value", 0));
        assertEquals(value, AutoObjectives.inject(List.of(optional), value));
        List<Objective> priority = List.of(new Objective("p", "maximize_task_priority", 2));
        assertEquals(priority, AutoObjectives.inject(List.of(optional), priority));
    }

    @Test
    void idIsMadeUniqueWhenTaken() {
        Objective taken = new Objective("auto-selected-value", "minimize_makespan", 1);
        List<Objective> result = AutoObjectives.inject(List.of(optional), List.of(taken));
        assertEquals("auto-selected-value-2", result.get(1).id());
    }

    @Test
    void nullInputsAreTreatedAsEmptyAndResultIsUnmodifiable() {
        assertTrue(AutoObjectives.inject(null, null).isEmpty());
        List<Objective> result = AutoObjectives.inject(List.of(optional), null);
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Objective("x", "minimize_makespan", 1)));
    }
}

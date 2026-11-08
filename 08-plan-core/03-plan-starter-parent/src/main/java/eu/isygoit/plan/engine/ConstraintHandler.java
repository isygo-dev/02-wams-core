package eu.isygoit.plan.engine;

import eu.isygoit.plan.domain.*;

public interface ConstraintHandler {
    String type();

    void validate(PlanningProblem problem, Constraint constraint);

    default boolean allowsAssignment(
            PlanningProblem problem, Constraint constraint, Task task, Resource resource, Slot slot) {
        return true;
    }

    default Integer resourceSlotCapacity(
            PlanningProblem problem, Constraint constraint, Resource resource, Slot slot) {
        return null;
    }
}

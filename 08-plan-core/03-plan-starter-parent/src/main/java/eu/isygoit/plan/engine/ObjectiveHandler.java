package eu.isygoit.plan.engine;

import eu.isygoit.plan.domain.Objective;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.Task;

public interface ObjectiveHandler {
    String type();

    void validate(Objective objective);

    default long assignmentCost(Objective objective, Task task, Resource resource, Slot slot) {
        return 0L;
    }
}

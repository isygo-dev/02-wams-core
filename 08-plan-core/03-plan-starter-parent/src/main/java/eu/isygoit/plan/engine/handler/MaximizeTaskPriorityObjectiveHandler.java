package eu.isygoit.plan.engine.handler;

import eu.isygoit.plan.domain.Objective;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.Task;
import eu.isygoit.plan.engine.ObjectiveHandler;
import org.springframework.stereotype.Component;

@Component
public class MaximizeTaskPriorityObjectiveHandler implements ObjectiveHandler {
    @Override
    public String type() {
        return "maximize_task_priority";
    }

    @Override
    public void validate(Objective objective) {
    }

    @Override
    public long assignmentCost(Objective objective, Task task, Resource resource, Slot slot) {
        Object value = task.attributes().get("priority");
        if (value == null) {
            return 0L;
        }
        return ObjectiveNumber.negatedRoundedLong(value, "Task '" + task.id() + "' priority");
    }
}

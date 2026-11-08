package eu.isygoit.plan.engine.handler;

import eu.isygoit.plan.domain.Objective;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.Task;
import eu.isygoit.plan.engine.ObjectiveHandler;
import org.springframework.stereotype.Component;

@Component
public class MinimizeResourceCostObjectiveHandler implements ObjectiveHandler {
    @Override
    public String type() {
        return "minimize_resource_cost";
    }

    @Override
    public void validate(Objective objective) {
    }

    @Override
    public long assignmentCost(Objective objective, Task task, Resource resource, Slot slot) {
        Object value = resource.attributes().get("cost");
        if (value == null) {
            return 0L;
        }
        return ObjectiveNumber.roundedLong(value, "Resource '" + resource.id() + "' cost");
    }
}

package eu.isygoit.plan.engine;

import eu.isygoit.plan.domain.Objective;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.Task;
import eu.isygoit.plan.exception.InvalidRequestException;
import eu.isygoit.plan.exception.UnknownObjectiveException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ObjectiveRegistry {
    private final Map<String, ObjectiveHandler> handlers;

    public ObjectiveRegistry(Collection<ObjectiveHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                ObjectiveHandler::type, Function.identity()));
    }

    public void verifyAll(Collection<Objective> objectives) {
        for (Objective objective : objectives) {
            ObjectiveHandler handler = handlers.get(objective.type());
            if (handler == null) {
                throw new UnknownObjectiveException(objective.type(), handlers.keySet());
            }
            handler.validate(objective);
        }
    }

    public Collection<String> supportedTypes() {
        return handlers.keySet();
    }

    public long assignmentCost(
            Collection<Objective> objectives, Task task, Resource resource, Slot slot) {
        long cost = 0L;
        try {
            for (Objective objective : objectives) {
                long contribution = handlers.get(objective.type()).assignmentCost(objective, task, resource, slot);
                cost = Math.addExact(cost, Math.multiplyExact(contribution, objective.weight()));
            }
        } catch (ArithmeticException exception) {
            throw new InvalidRequestException(
                    "OBJECTIVE_SCORE_OUT_OF_RANGE", "Combined planning objective exceeds the supported range");
        }
        return cost;
    }
}

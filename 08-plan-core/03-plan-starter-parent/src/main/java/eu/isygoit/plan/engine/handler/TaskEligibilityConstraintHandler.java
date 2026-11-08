package eu.isygoit.plan.engine.handler;

import eu.isygoit.plan.domain.*;
import eu.isygoit.plan.engine.ConstraintHandler;
import eu.isygoit.plan.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TaskEligibilityConstraintHandler implements ConstraintHandler {
    @Override
    public String type() {
        return "task_eligibility";
    }

    @Override
    public void validate(PlanningProblem problem, Constraint constraint) {
        if (constraint.scope() == null || !"task".equalsIgnoreCase(constraint.scope().kind())
                || problem.tasks().stream().noneMatch(task -> task.id().equals(constraint.scope().value()))) {
            throw invalid("task_eligibility requires a scope referencing an existing task");
        }
        validateIds(constraint.params().get("resourceIds"), "resourceIds");
        validateIds(constraint.params().get("slotIds"), "slotIds");
        validateReferences(constraint.params().get("resourceIds"),
                problem.resources().stream().map(Resource::id).collect(java.util.stream.Collectors.toSet()),
                "resourceIds");
        validateReferences(constraint.params().get("slotIds"),
                problem.slots().stream().map(Slot::id).collect(java.util.stream.Collectors.toSet()),
                "slotIds");
    }

    @Override
    public boolean allowsAssignment(
            PlanningProblem problem, Constraint constraint, Task task, Resource resource, Slot slot) {
        if (!constraint.scope().value().equals(task.id())) {
            return true;
        }
        Map<String, Object> params = constraint.params();
        return allows(params.get("resourceIds"), resource.id())
                && allows(params.get("slotIds"), slot.id());
    }

    private boolean allows(Object value, String id) {
        if (value == null) {
            return true;
        }
        return ((Collection<?>) value).contains(id);
    }

    private void validateIds(Object value, String parameter) {
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?> values)
                || values.stream().anyMatch(id -> !(id instanceof String string) || string.isBlank())) {
            throw invalid("task_eligibility " + parameter + " must be a list of non-empty strings");
        }
    }

    private void validateReferences(Object value, Set<String> available, String parameter) {
        if (value instanceof Collection<?> values
                && values.stream().map(String.class::cast).anyMatch(id -> !available.contains(id))) {
            throw invalid("task_eligibility " + parameter + " references an unknown id");
        }
    }

    private InvalidRequestException invalid(String message) {
        return new InvalidRequestException("INVALID_CONSTRAINT", message);
    }
}

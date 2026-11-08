package eu.isygoit.plan.engine.handler;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.plan.domain.Constraint;
import eu.isygoit.plan.domain.PlanningProblem;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.engine.ConstraintHandler;
import eu.isygoit.plan.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResourceCapacityConstraintHandler implements ConstraintHandler {
    @Override
    public String type() {
        return "resource_capacity";
    }

    @Override
    public void validate(PlanningProblem problem, Constraint constraint) {
        if (constraint.enforcement() == IEnumEnforcement.Types.SOFT) {
            throw invalid("resource_capacity does not support SOFT enforcement");
        }
        int capacity = capacity(constraint.params());
        if (capacity < 1) {
            throw invalid("resource_capacity capacity must be positive");
        }
        if (constraint.scope() != null
                && (!"resource".equalsIgnoreCase(constraint.scope().kind())
                || problem.resources().stream().noneMatch(
                resource -> resource.id().equals(constraint.scope().value())))) {
            throw invalid("resource_capacity scope must reference an existing resource");
        }
    }

    @Override
    public Integer resourceSlotCapacity(
            PlanningProblem problem, Constraint constraint, Resource resource, Slot slot) {
        if (constraint.scope() != null && !constraint.scope().value().equals(resource.id())) {
            return null;
        }
        return capacity(constraint.params());
    }

    private int capacity(Map<String, Object> params) {
        Object value = params.get("capacity");
        if (!(value instanceof Number number)
                || number.doubleValue() != number.intValue()) {
            throw invalid("resource_capacity requires an integer capacity parameter");
        }
        return number.intValue();
    }

    private InvalidRequestException invalid(String message) {
        return new InvalidRequestException("INVALID_CONSTRAINT", message);
    }
}

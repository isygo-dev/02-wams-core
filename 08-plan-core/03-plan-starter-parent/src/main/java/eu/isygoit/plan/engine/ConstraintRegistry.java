package eu.isygoit.plan.engine;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.plan.domain.*;
import eu.isygoit.plan.exception.InvalidRequestException;
import eu.isygoit.plan.exception.UnknownConstraintException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConstraintRegistry {
    private final Map<String, ConstraintHandler> handlers;

    public ConstraintRegistry(Collection<ConstraintHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                ConstraintHandler::type, Function.identity()));
    }

    public void verifyAll(PlanningProblem problem) {
        for (Constraint constraint : problem.constraints()) {
            ConstraintHandler handler = handlers.get(constraint.type());
            if (handler == null) {
                throw new UnknownConstraintException(constraint.type(), handlers.keySet());
            }
            handler.validate(problem, constraint);
        }
    }

    public Collection<String> supportedTypes() {
        return handlers.keySet();
    }

    public boolean allowsAssignment(
            PlanningProblem problem,
            Task task,
            Resource resource,
            Slot slot,
            Collection<String> relaxedConstraintIds) {
        for (Constraint constraint : problem.constraints()) {
            if (constraint.enforcement() == IEnumEnforcement.Types.SOFT
                    || relaxedConstraintIds.contains(constraint.id())) {
                continue;
            }
            if (!handlers.get(constraint.type()).allowsAssignment(problem, constraint, task, resource, slot)) {
                return false;
            }
        }
        return true;
    }

    public long assignmentPenalty(
            PlanningProblem problem, Task task, Resource resource, Slot slot) {
        long penalty = 0L;
        try {
            for (Constraint constraint : problem.constraints()) {
                if (constraint.enforcement() == IEnumEnforcement.Types.SOFT
                        && !handlers.get(constraint.type())
                        .allowsAssignment(problem, constraint, task, resource, slot)) {
                    penalty = Math.addExact(penalty, constraint.weight());
                }
            }
        } catch (ArithmeticException exception) {
            throw new InvalidRequestException(
                    "OBJECTIVE_SCORE_OUT_OF_RANGE", "Combined soft constraint penalty exceeds the supported range");
        }
        return penalty;
    }

    public Integer resourceSlotCapacity(
            PlanningProblem problem, Resource resource, Slot slot, Collection<String> relaxedConstraintIds) {
        Integer capacity = null;
        for (Constraint constraint : problem.constraints()) {
            if (constraint.enforcement() == IEnumEnforcement.Types.SOFT
                    || relaxedConstraintIds.contains(constraint.id())) {
                continue;
            }
            Integer constraintCapacity =
                    handlers.get(constraint.type()).resourceSlotCapacity(problem, constraint, resource, slot);
            if (constraintCapacity != null) {
                capacity = capacity == null ? constraintCapacity : Math.min(capacity, constraintCapacity);
            }
        }
        return capacity;
    }

    public Collection<Constraint> violatedSoftConstraints(
            PlanningProblem problem, Task task, Resource resource, Slot slot) {
        return problem.constraints().stream()
                .filter(constraint -> constraint.enforcement() == IEnumEnforcement.Types.SOFT)
                .filter(constraint -> !handlers.get(constraint.type())
                        .allowsAssignment(problem, constraint, task, resource, slot))
                .toList();
    }
}

package eu.isygoit.plan.application;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.plan.domain.*;
import eu.isygoit.plan.engine.ConstraintRegistry;
import eu.isygoit.plan.exception.InvalidSolverResultException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class PlanningSolutionValidator {
    private final ConstraintRegistry constraintRegistry;

    public PlanningSolutionValidator(ConstraintRegistry constraintRegistry) {
        this.constraintRegistry = constraintRegistry;
    }

    public void validate(PlanningProblem problem, SolveResult result) {
        if (result.solution() == null) {
            throw new InvalidSolverResultException("A feasible solver result must include a solution");
        }
        Map<String, Task> tasks = new HashMap<>();
        problem.tasks().forEach(task -> tasks.put(task.id(), task));
        Map<String, Resource> resources = new HashMap<>();
        problem.resources().forEach(resource -> resources.put(resource.id(), resource));
        Map<String, Slot> slots = new HashMap<>();
        problem.slots().forEach(slot -> slots.put(slot.id(), slot));
        Set<String> relaxedIds = result.relaxations().stream()
                .map(relaxation -> relaxation.constraintId())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> assigned = new HashSet<>();
        Map<String, UnassignedTask> unassigned = new HashMap<>();
        for (UnassignedTask unassignedTask : result.solution().unassignedTasks()) {
            Task task = tasks.get(unassignedTask.taskId());
            if (task == null || task.requiredTask()
                    || unassigned.putIfAbsent(unassignedTask.taskId(), unassignedTask) != null) {
                throw new InvalidSolverResultException(
                        "Solver returned an invalid unassigned-task entry");
            }
        }
        Map<String, Integer> resourceSlotCounts = new HashMap<>();
        for (Assignment assignment : result.solution().assignments()) {
            Task task = tasks.get(assignment.taskId());
            if (task == null) {
                throw new InvalidSolverResultException(
                        "Solver returned an assignment for unknown task '" + assignment.taskId() + "'");
            }
            if (!assigned.add(assignment.taskId())) {
                throw new InvalidSolverResultException(
                        "Solver assigned task '" + assignment.taskId() + "' more than once");
            }
            Slot slot = slots.get(assignment.slotId());
            if (slot == null || assignment.resources().isEmpty()
                    || !slot.id().equals(assignment.slotId())
                    || !java.util.Objects.equals(slot.start(), assignment.start())
                    || !java.util.Objects.equals(slot.end(), assignment.end())) {
                throw new InvalidSolverResultException(
                        "Solver returned an assignment without a valid slot and resource");
            }
            for (var resourceAssignment : assignment.resources()) {
                Resource resource = resources.get(resourceAssignment.resourceId());
                if (resource == null) {
                    throw new InvalidSolverResultException(
                            "Solver returned an unknown resource '" + resourceAssignment.resourceId() + "'");
                }
                if (resource.unavailableSlots().contains(slot.id())
                        || !matches(task.candidates().resourceSelectors(), resource.id())
                        || !matches(task.candidates().slotSelectors(), slot.id())
                        || !hasCapabilities(task, resource)
                        || !constraintRegistry.allowsAssignment(problem, task, resource, slot, relaxedIds)) {
                    throw new InvalidSolverResultException(
                            "Solver returned an assignment that violates a hard eligibility rule");
                }
                String key = resource.id() + "|" + slot.id();
                resourceSlotCounts.merge(key, 1, Integer::sum);
            }
        }
        for (Task task : tasks.values()) {
            boolean isAssigned = assigned.contains(task.id());
            boolean isUnassigned = unassigned.containsKey(task.id());
            if ((task.requiredTask() && !isAssigned)
                    || (!task.requiredTask() && isAssigned == isUnassigned)) {
                throw new InvalidSolverResultException(
                        "Solver returned an inconsistent assignment state for task '" + task.id() + "'");
            }
        }
        for (Resource resource : resources.values()) {
            for (Slot slot : slots.values()) {
                int count = resourceSlotCounts.getOrDefault(resource.id() + "|" + slot.id(), 0);
                if (count > capacity(problem, resource, slot, relaxedIds)) {
                    throw new InvalidSolverResultException(
                            "Solver exceeded capacity for resource '" + resource.id() + "'");
                }
            }
        }
        for (var relaxation : result.relaxations()) {
            Constraint constraint = problem.constraints().stream()
                    .filter(item -> item.id().equals(relaxation.constraintId()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidSolverResultException(
                            "Solver returned an unknown relaxed constraint"));
            if (constraint.enforcement() == IEnumEnforcement.Types.STRICTLY_REQUIRED
                    || constraint.enforcement() != IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                    || !constraint.relaxation().authorized()) {
                throw new InvalidSolverResultException(
                        "Solver attempted to relax a constraint that is not authorized for relaxation");
            }
        }
    }

    private int capacity(PlanningProblem problem, Resource resource, Slot slot, Set<String> relaxedIds) {
        Object value = resource.attributes().get("capacity");
        int resourceCapacity = value == null ? Integer.MAX_VALUE : ((Number) value).intValue();
        Integer constraintCapacity =
                constraintRegistry.resourceSlotCapacity(problem, resource, slot, relaxedIds);
        int configuredCapacity = constraintCapacity == null ? Integer.MAX_VALUE : constraintCapacity;
        int capacity = Math.min(resourceCapacity, configuredCapacity);
        return capacity == Integer.MAX_VALUE ? 1 : capacity;
    }

    private boolean matches(java.util.List<String> selectors, String id) {
        return selectors.isEmpty() || selectors.stream().anyMatch(selector ->
                "ALL".equalsIgnoreCase(selector) || selector.equals(id));
    }

    private boolean hasCapabilities(Task task, Resource resource) {
        java.util.List<String> required = task.candidates().capabilityTags();
        if (required.isEmpty()) {
            return true;
        }
        Object value = resource.attributes().get("capabilities");
        return value instanceof java.util.List<?> capabilities && capabilities.containsAll(required);
    }
}

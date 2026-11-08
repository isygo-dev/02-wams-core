package eu.isygoit.plan.application;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.plan.api.DomainDefinition;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.api.TypeDefinition;
import eu.isygoit.plan.domain.Constraint;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.Task;
import eu.isygoit.plan.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PlanningRequestValidator {
    public void validate(PlanningRequest request) {
        if (request == null || request.meta() == null) {
            throw invalid("Request meta is required");
        }
        if (request.meta().jobId() == null) {
            throw invalid("meta.jobId is required");
        }
        requireText(request.meta().version(), "meta.version");
        requireText(request.meta().domainName(), "meta.domainName");
        if (request.domain() == null) {
            throw invalid("domain is required");
        }

        DomainDefinition domain = request.domain();
        Set<String> resourceTypes = typeIds(domain.resourceTypes(), "domain.resourceTypes");
        Set<String> taskTypes = typeIds(domain.taskTypes(), "domain.taskTypes");
        Set<String> slotTypes = typeIds(domain.slotTypes(), "domain.slotTypes");
        List<Task> tasks = request.tasks() == null ? List.of() : request.tasks();
        List<Resource> resources = request.resources() == null ? List.of() : request.resources();
        List<Slot> slots = request.slots() == null ? List.of() : request.slots();
        List<Constraint> constraints = request.constraints() == null ? List.of() : request.constraints();
        List<eu.isygoit.plan.domain.Objective> objectives =
                request.objectives() == null ? List.of() : request.objectives();
        requireNoNulls(tasks, "tasks");
        requireNoNulls(resources, "resources");
        requireNoNulls(slots, "slots");
        requireNoNulls(constraints, "constraints");
        requireNoNulls(objectives, "objectives");

        Set<String> taskIds = uniqueIds(tasks.stream().map(Task::id).toList(), "task");
        Set<String> resourceIds = uniqueIds(resources.stream().map(Resource::id).toList(), "resource");
        Set<String> slotIds = uniqueIds(slots.stream().map(Slot::id).toList(), "slot");
        uniqueIds(constraints.stream().map(Constraint::id).toList(), "constraint");
        uniqueIds(objectives.stream().map(objective -> objective.id()).toList(), "objective");

        for (Task task : tasks) {
            requireType(task.typeId(), taskTypes, "task " + task.id());
            if (task.candidates() != null) {
                validateSelectors(task.candidates().resourceSelectors(), resourceIds, "resource", task.id());
                validateSelectors(task.candidates().slotSelectors(), slotIds, "slot", task.id());
                validateTextValues(task.candidates().capabilityTags(), "capability", task.id());
            }
        }
        for (Resource resource : resources) {
            requireType(resource.typeId(), resourceTypes, "resource " + resource.id());
            validateSelectors(resource.unavailableSlots(), slotIds, "unavailable slot", resource.id());
            Object capacity = resource.attributes().get("capacity");
            if (capacity != null && (!(capacity instanceof Number number)
                    || !Double.isFinite(number.doubleValue())
                    || number.doubleValue() != number.intValue()
                    || number.intValue() < 1)) {
                throw invalid("resource " + resource.id() + " capacity must be a positive integer");
            }
            Object capabilities = resource.attributes().get("capabilities");
            if (capabilities != null && (!(capabilities instanceof List<?> values)
                    || values.stream().anyMatch(value -> !(value instanceof String text) || text.isBlank()))) {
                throw invalid("resource " + resource.id() + " capabilities must be a list of non-empty strings");
            }
        }
        for (Slot slot : slots) {
            requireType(slot.typeId(), slotTypes, "slot " + slot.id());
            if (slot.start() == null || slot.end() == null || !slot.start().isBefore(slot.end())) {
                throw invalid("slot " + slot.id() + " must have start before end");
            }
        }
        for (Constraint constraint : constraints) {
            requireText(constraint.id(), "constraint.id");
            requireText(constraint.type(), "constraint.type");
            if (constraint.enforcement() == null) {
                throw invalid("constraint " + constraint.id() + " must specify enforcement");
            }
            if (constraint.enforcement() == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                    && (constraint.relaxation() == null || !constraint.relaxation().authorized())) {
                throw invalid("constraint " + constraint.id()
                        + " requires an authorized relaxation policy");
            }
        }
        if (request.solverOptions() != null) {
            requireText(request.solverOptions().engine(), "solverOptions.engine");
            if (request.solverOptions().numWorkers() < 1 || request.solverOptions().numWorkers() > 32) {
                throw invalid("solverOptions.numWorkers must be between 1 and 32");
            }
            if (request.solverOptions().maxTimeSeconds() < 1) {
                throw invalid("solverOptions.maxTimeSeconds must be positive");
            }
        }
    }

    private Set<String> typeIds(List<TypeDefinition> definitions, String path) {
        if (definitions == null) {
            return Set.of();
        }
        Set<String> ids = new HashSet<>();
        for (TypeDefinition definition : definitions) {
            if (definition == null || definition.id() == null || definition.id().isBlank()
                    || !ids.add(definition.id())) {
                throw invalid(path + " must contain unique non-empty type ids");
            }
        }
        return ids;
    }

    private Set<String> uniqueIds(List<String> values, String kind) {
        Set<String> ids = new HashSet<>();
        for (String id : values) {
            if (id == null || id.isBlank() || !ids.add(id)) {
                throw invalid(kind + " ids must be unique and non-empty");
            }
        }
        return ids;
    }

    private void requireNoNulls(List<?> values, String path) {
        if (values.stream().anyMatch(value -> value == null)) {
            throw invalid(path + " must not contain null entries");
        }
    }

    private void requireType(String typeId, Set<String> allowed, String path) {
        if (typeId == null || !allowed.contains(typeId)) {
            throw invalid(path + " references unknown type '" + typeId + "'");
        }
    }

    private void validateSelectors(List<String> selectors, Set<String> available, String kind, String owner) {
        if (selectors == null) {
            return;
        }
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                throw invalid(owner + " has an empty " + kind + " selector");
            }
            if (!"ALL".equalsIgnoreCase(selector) && !available.contains(selector)) {
                throw invalid(owner + " references unknown " + kind + " '" + selector + "'");
            }
        }
    }

    private void validateTextValues(List<String> values, String kind, String owner) {
        if (values != null && values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw invalid(owner + " has an empty " + kind + " value");
        }
    }

    private void requireText(String value, String path) {
        if (value == null || value.isBlank()) {
            throw invalid(path + " is required");
        }
    }

    private InvalidRequestException invalid(String message) {
        return new InvalidRequestException("INVALID_REQUEST", message);
    }
}

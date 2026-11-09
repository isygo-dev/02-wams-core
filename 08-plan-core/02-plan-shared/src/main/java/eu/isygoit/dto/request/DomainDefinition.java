package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * OPTIONAL UI hints of a request: the type ids (and attribute names) of resources, tasks and slots. When given, the
 * {@code typeId} fields of the entities are validated against it. Never read by the engines.
 * <pre>{@code
 * { "resourceTypes": [{ "id": "teacher", "attributes": ["name"] }],
 *   "taskTypes": [{ "id": "lesson", "attributes": ["classId", "subject"] }],
 *   "slotTypes": [{ "id": "period", "attributes": ["day"] }] }
 * }</pre>
 *
 * @param resourceTypes resource type definitions, default empty
 * @param taskTypes     task type definitions, default empty
 * @param slotTypes     slot type definitions, default empty
 */
public record DomainDefinition(
        List<TypeDefinition> resourceTypes,
        List<TypeDefinition> taskTypes,
        List<TypeDefinition> slotTypes) {

    public DomainDefinition {
        resourceTypes = CollectionCopies.list(resourceTypes);
        taskTypes = CollectionCopies.list(taskTypes);
        slotTypes = CollectionCopies.list(slotTypes);
    }
}

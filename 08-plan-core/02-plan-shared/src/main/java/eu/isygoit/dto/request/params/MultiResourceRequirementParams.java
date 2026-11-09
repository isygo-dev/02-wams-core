package eu.isygoit.dto.request.params;

import eu.isygoit.dto.request.ResourceSelector;

/**
 * Params of {@code multi_resource_requirement}: adds or replaces the requirement {@code role} on every scoped task
 * (bulk definition; applied as a task rewrite by the normalizer).
 * <pre>{@code
 * { "role": "room", "selector": { "resourceTypeIds": ["room"] }, "count": 1 }
 * }</pre>
 *
 * @param role     role of the requirement to add/replace
 * @param selector who may fulfil it
 * @param count    number of distinct resources, default 1
 * @param amount   capacity amount consumed on each, null = task demand
 */
public record MultiResourceRequirementParams(String role, ResourceSelector selector, Integer count, Integer amount) {
}

package eu.isygoit.dto.request.params;

/**
 * Params of {@code same_resource} and {@code different_resource}: the tasks of a group (task tags with the given
 * prefix, or the scope tag) use the same / pairwise different resources for {@code role} (all roles when null).
 * <pre>{@code
 * { "groupByTagPrefix": "order-", "role": "operator" }
 * }</pre>
 *
 * @param groupByTagPrefix tag prefix defining the groups (null when the scope is a tag)
 * @param role             requirement role the rule applies to, null = all roles
 */
public record GroupingParams(String groupByTagPrefix, String role) {
}

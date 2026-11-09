package eu.isygoit.dto.request;

/**
 * One resource demand of a task: "I need {@code count} distinct resources matching {@code selector}, each consuming
 * {@code amount} of its capacity". A task may have several requirements (teacher + room + operator), each with its
 * own role.
 * <pre>{@code
 * { "role": "teacher", "selector": { "skills": ["math"] }, "count": 1 }
 * }</pre>
 * Defaults applied here: {@code role} = {@value #DEFAULT_ROLE}, {@code selector} = every resource,
 * {@code count} = 1. {@code amount} stays null (null means "the task demand"; resolved by the normalizer).
 * During normalization, when the role exactly matches a type id present on a resource and the selector has no
 * {@code resourceTypeIds}, that type is inferred. Supply {@code resourceTypeIds} explicitly to override inference.
 * Range checks (count/amount {@code >= 1}, unique roles) belong to the request validator.
 *
 * @param role     label echoed in the result; matching resource type ids are inferred when selector types are omitted
 * @param selector who may fulfil it
 * @param count    number of distinct resources, {@code >= 1}
 * @param amount   capacity amount consumed on each, {@code >= 1}; null = {@code Task.demand}
 */
public record ResourceRequirement(
        String role,
        ResourceSelector selector,
        Integer count,
        Integer amount) {

    /**
     * Role of the implicit requirement of a task that declares none.
     */
    public static final String DEFAULT_ROLE = "default";

    public ResourceRequirement {
        role = role == null || role.isBlank() ? DEFAULT_ROLE : role;
        selector = selector == null ? ResourceSelector.all() : selector;
        count = count == null ? Integer.valueOf(1) : count;
    }

    /**
     * @return the implicit requirement of a task without requirements: role "default", count 1, selector ALL
     */
    public static ResourceRequirement implicit() {
        return new ResourceRequirement(DEFAULT_ROLE, ResourceSelector.all(), 1, null);
    }
}

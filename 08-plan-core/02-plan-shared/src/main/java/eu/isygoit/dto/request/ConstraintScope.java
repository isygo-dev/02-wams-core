package eu.isygoit.dto.request;

/**
 * Selects the tasks or resources a constraint or objective looks at. Resolved ONCE per constraint by the candidate
 * index.
 * <p>
 * {@code kind} is one of: {@code "all"} (value ignored), {@code "task"} (task id), {@code "taskType"},
 * {@code "resource"} (resource id), {@code "resourceId"} (alias of resource), {@code "resourceType"},
 * {@code "tag"} (a TASK tag; for resource-oriented families it selects RESOURCE tags, documented per family).
 * A null/blank kind means {@code "all"}.
 * <pre>{@code
 * { "kind": "tag", "value": "class-7A" }
 * }</pre>
 *
 * @param kind  scope kind, default {@value #ALL}
 * @param value the id/type/tag the kind refers to
 */
public record ConstraintScope(String kind, String value) {

    /**
     * Kind of the default scope.
     */
    public static final String ALL = "all";

    public ConstraintScope {
        kind = kind == null || kind.isBlank() ? ALL : kind;
    }

    /**
     * @return the default scope {@code {kind:"all"}}
     */
    public static ConstraintScope all() {
        return new ConstraintScope(ALL, null);
    }
}

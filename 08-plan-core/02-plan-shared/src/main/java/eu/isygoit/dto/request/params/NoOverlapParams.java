package eu.isygoit.dto.request.params;

/**
 * Params of {@code no_overlap}: tasks of a group never overlap in time (class/patient conflicts, mutual exclusion
 * regardless of resource), with {@code minGapTicks} between them. One group per task tag starting with
 * {@code groupByTagPrefix}; with {@code scope.kind="tag"} the scope itself is ONE group.
 * <pre>{@code
 * { "groupByTagPrefix": "class-" }
 * }</pre>
 * {@code minGapTicks} defaults to 0.
 *
 * @param groupByTagPrefix tag prefix defining the groups (null when the scope is a tag)
 * @param minGapTicks      minimum idle ticks between two tasks of a group, default 0
 */
public record NoOverlapParams(String groupByTagPrefix, Integer minGapTicks) {

    public NoOverlapParams {
        minGapTicks = minGapTicks == null ? Integer.valueOf(0) : minGapTicks;
    }
}

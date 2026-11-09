package eu.isygoit.dto.request.params;

import eu.isygoit.dto.request.ConstraintScope;

/**
 * Params of {@code min_rest}: gap of at least {@code minRestTicks} between consecutive tasks of a (unary)
 * resource. {@code minRestTicks} is required (primitive).
 * <pre>{@code
 * { "minRestTicks": 11 }
 * }</pre>
 *
 * @param minRestTicks minimum gap in ticks
 * @param afterTasks   only tasks selected by this scope are followed by a rest; null = all tasks
 */
public record MinRestParams(int minRestTicks, ConstraintScope afterTasks) {
}

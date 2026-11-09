package eu.isygoit.solver;

import eu.isygoit.dto.response.Assignment;

import java.util.List;

/**
 * Immutable snapshot of the best solution found so far, delivered to {@link SolveListener#onIncumbent} only when the
 * listener asked for incumbents.
 *
 * @param elapsedMillis wall time since the start of the whole request, in milliseconds
 * @param assignments   the assignments of the incumbent (unmodifiable copy)
 * @param objective     objective value in scaled units
 */
public record IncumbentSnapshot(long elapsedMillis, List<Assignment> assignments, double objective) {

    public IncumbentSnapshot {
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }
}

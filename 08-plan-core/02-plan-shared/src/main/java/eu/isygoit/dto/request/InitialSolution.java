package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Warm start / locked assignments of a particular run (replanning). {@code Task.pin} is a definition-level fact;
 * the initial solution is about this run only.
 *
 * @param assignments previous assignments (hints or locks)
 */
public record InitialSolution(List<InitialAssignment> assignments) {

    public InitialSolution {
        assignments = CollectionCopies.list(assignments);
    }
}

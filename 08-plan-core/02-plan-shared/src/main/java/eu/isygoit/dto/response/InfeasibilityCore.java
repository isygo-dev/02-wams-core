package eu.isygoit.dto.response;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Structured IIS-style explanation of an INFEASIBLE result. Items are sorted by kind (CONSTRAINT, TASK, RESOURCE,
 * EDGE) then id.
 *
 * @param minimal true when no item can be removed without making the rest feasible
 * @param method  PRESOLVE_ANALYSIS (exact, no solver) | ASSUMPTIONS (CP-SAT sufficient assumptions) |
 *                ASSUMPTIONS_SHRUNK (deletion filter applied) | NONE
 * @param items   the elements of the core
 */
public record InfeasibilityCore(boolean minimal, String method, List<CoreItem> items) {

    public InfeasibilityCore {
        items = CollectionCopies.list(items);
    }
}

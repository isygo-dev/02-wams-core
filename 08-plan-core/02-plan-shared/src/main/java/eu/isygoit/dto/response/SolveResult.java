package eu.isygoit.dto.response;

import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Outcome of one solve. This is the shape persisted in the job result column and returned by
 * {@code GET /{id}/result}.
 * <ul>
 *   <li>OPTIMAL: proven optimal for every tier. FEASIBLE: a solution exists, time/gap limit reached.</li>
 *   <li>STOPPED_FEASIBLE: a stop was requested while an incumbent existed; the incumbent is returned.</li>
 *   <li>INFEASIBLE: proven (see {@link Diagnostics#core()}). UNKNOWN: nothing found, nothing proven.</li>
 * </ul>
 *
 * @param status      solver status
 * @param solution    null for INFEASIBLE / UNKNOWN (and STOPPED without incumbent)
 * @param relaxations constraints dropped AND actually violated, with real impact
 * @param diagnostics message, infeasibility core, warnings, details
 * @param metrics     timing and search statistics
 */
public record SolveResult(
        IEnumSolverStatus.Types status,
        PlanningSolution solution,
        List<Relaxation> relaxations,
        Diagnostics diagnostics,
        Metrics metrics) {

    public SolveResult {
        relaxations = CollectionCopies.list(relaxations);
    }
}

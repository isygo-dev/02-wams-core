package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.solver.index.CandidateIndexBuilder;

/**
 * Handle given to {@link ConstraintFamily#preprocess}: the engine-neutral compile step in which a HARD family prunes
 * candidates and start domains (so that both engines benefit).
 * <p>
 * Implemented by {@code CandidateIndexBuilder} (WP2); called on the building thread only (not thread-safe). Indices
 * are the build-time task / resource / candidate indices (request order); all times are integer ticks, start-domain
 * bounds are INCLUSIVE tick values.
 */
public interface PreprocessContext {

    /** @return the mutable build state: prune (task, requirement, resource) / start options, rewrite requirements */
    CandidateIndexBuilder.Mutable candidates();

    /** @return the task indices selected by the scope, ascending (cached) */
    int[] resolveTasks(ConstraintScope scope);

    /** @return the resource indices selected by the scope, ascending (cached) */
    int[] resolveResources(ConstraintScope scope);

    /** @return the problem horizon */
    Horizon horizon();

    /**
     * Removes a candidate and records why, for the infeasibility core.
     *
     * @param candidate    candidate index
     * @param reasonCode   core reason code (for example {@code CONSTRAINT})
     * @param constraintId id of the constraint that pruned it
     */
    void pruneCandidate(int candidate, String reasonCode, String constraintId);

    /** Intersects the start domain of {@code task} with the inclusive tick range {@code [lo, hi]}. */
    void restrictStart(int task, int lo, int hi);

    /**
     * Intersects the start domain of {@code task} with a union of inclusive runs.
     *
     * @param runs {@code [lo0,hi0,lo1,hi1,...]} ascending, inclusive bounds, even length
     */
    void restrictStartDomain(int task, int[] runs);
}

package eu.isygoit.dto.response;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * A relaxable constraint that was dropped AND is actually violated by the solution. Constraints that were dropped
 * but are harmlessly satisfied are not reported at all.
 *
 * @param constraintId     the relaxed constraint
 * @param originalValue    enforcement name before relaxation (kept from v1)
 * @param relaxedValue     enforcement name after relaxation (kept from v1)
 * @param impactOnScore    the measure (total violated family units); the real impact, never a guess
 * @param authorized       whether the relaxation policy authorised it
 * @param violationCount   number of instance violations found by the checker; 0 means not reported at all
 * @param violatedTaskIds  first 50 violating task ids, sorted
 * @param measure          family measure units violated (ticks over, tasks over, ...)
 */
public record Relaxation(
        String constraintId,
        Object originalValue,
        Object relaxedValue,
        double impactOnScore,
        boolean authorized,
        int violationCount,
        List<String> violatedTaskIds,
        double measure) {

    public Relaxation {
        violatedTaskIds = CollectionCopies.list(violatedTaskIds);
    }
}

package eu.isygoit.solver.check;

import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceSelector;

/**
 * Straightforward, index-free evaluation of a {@link ResourceSelector} on one {@link Resource}.
 * <p>
 * The independent validator and the pre-solve analyzer deliberately do NOT reuse the CandidateIndex bitset evaluator
 * (WP2 {@code SelectorEvaluator}): re-deriving eligibility from the raw problem is what makes the post-check
 * independent. Semantics (design 2.1): non-empty {@code resourceIds} restrict to those ids (the literal {@code "ALL"}
 * matches everything), non-empty {@code resourceTypeIds} to those types, {@code skills} must ALL be present,
 * {@code anySkills} at least one (empty = ignored), {@code tags} must ALL be carried, {@code excludeResourceIds} removes.
 * <p>
 * Stateless and thread-safe.
 */
public final class SelectorMatcher {

    /** Literal accepted inside {@code resourceIds} meaning "every resource". */
    public static final String ALL = "ALL";

    private SelectorMatcher() {
    }

    /**
     * @param selector the selector (null = everything)
     * @param resource the resource under test
     * @return {@code true} when the resource may fulfil a requirement carrying the selector
     */
    public static boolean matches(ResourceSelector selector, Resource resource) {
        if (selector == null) {
            return true;
        }
        if (!selector.excludeResourceIds().isEmpty() && selector.excludeResourceIds().contains(resource.id())) {
            return false;
        }
        if (!selector.resourceIds().isEmpty()
                && !selector.resourceIds().contains(ALL)
                && !selector.resourceIds().contains(resource.id())) {
            return false;
        }
        if (!selector.resourceTypeIds().isEmpty() && !selector.resourceTypeIds().contains(resource.typeId())) {
            return false;
        }
        if (!resource.skills().containsAll(selector.skills())) {
            return false;
        }
        if (!selector.anySkills().isEmpty()) {
            boolean any = false;
            for (String skill : selector.anySkills()) {
                if (resource.skills().contains(skill)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        return resource.tags().containsAll(selector.tags());
    }
}

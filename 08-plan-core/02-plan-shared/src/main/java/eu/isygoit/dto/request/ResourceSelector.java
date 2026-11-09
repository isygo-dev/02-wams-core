package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Pure data selector deciding which resources may fulfil a requirement. Evaluated once per distinct selector in the
 * candidate index (set algebra on bitsets). All empty lists = every resource.
 * <p>
 * Order of evaluation: ids/types/skills/anySkills/tags narrow the set (logical AND of the non-empty criteria),
 * then {@code excludeResourceIds} are removed.
 * <pre>{@code
 * { "skills": ["math"], "resourceTypeIds": ["teacher"], "excludeResourceIds": ["teacher-bob"] }
 * }</pre>
 *
 * @param resourceIds        non-empty = restrict to these ids (also accepts the literal {@code "ALL"})
 * @param resourceTypeIds    non-empty = restrict to these types
 * @param skills             the resource must have ALL of them
 * @param anySkills          the resource must have AT LEAST ONE (empty = ignored)
 * @param tags               the resource must carry ALL of these tags
 * @param excludeResourceIds removed after the criteria above
 */
public record ResourceSelector(
        List<String> resourceIds,
        List<String> resourceTypeIds,
        List<String> skills,
        List<String> anySkills,
        List<String> tags,
        List<String> excludeResourceIds) {

    public ResourceSelector {
        resourceIds = CollectionCopies.list(resourceIds);
        resourceTypeIds = CollectionCopies.list(resourceTypeIds);
        skills = CollectionCopies.list(skills);
        anySkills = CollectionCopies.list(anySkills);
        tags = CollectionCopies.list(tags);
        excludeResourceIds = CollectionCopies.list(excludeResourceIds);
    }

    /**
     * @return the selector that matches every resource (all criteria empty)
     */
    public static ResourceSelector all() {
        return new ResourceSelector(null, null, null, null, null, null);
    }
}

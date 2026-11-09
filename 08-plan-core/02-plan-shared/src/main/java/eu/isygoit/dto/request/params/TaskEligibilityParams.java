package eu.isygoit.dto.request.params;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Params of {@code task_eligibility}: scoped tasks may only use resources / slots matching these criteria (empty
 * lists = no restriction on that axis).
 * <pre>{@code
 * { "skills": ["math"], "excludeResourceIds": ["teacher-bob"], "slotIds": ["mon-p1"] }
 * }</pre>
 *
 * @param resourceIds        allowed resource ids
 * @param excludeResourceIds forbidden resource ids
 * @param resourceTypeIds    allowed resource types
 * @param skills             required skills
 * @param slotIds            allowed slot ids (slot mode)
 */
public record TaskEligibilityParams(
        List<String> resourceIds,
        List<String> excludeResourceIds,
        List<String> resourceTypeIds,
        List<String> skills,
        List<String> slotIds) {

    public TaskEligibilityParams {
        resourceIds = CollectionCopies.list(resourceIds);
        excludeResourceIds = CollectionCopies.list(excludeResourceIds);
        resourceTypeIds = CollectionCopies.list(resourceTypeIds);
        skills = CollectionCopies.list(skills);
        slotIds = CollectionCopies.list(slotIds);
    }
}

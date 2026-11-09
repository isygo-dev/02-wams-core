package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;
import eu.isygoit.util.ImmutableJsonMap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A unit of work (job, operation, shift, lesson, project activity). Time quantities are TICKS
 * ({@link Horizon}); time points are instants that must be tick aligned.
 * <pre>{@code
 * { "id": "A2", "duration": 20, "tags": ["job-A"],
 *   "requirements": [{ "role": "machine", "selector": { "resourceIds": ["M2"] } }],
 *   "predecessors": [{ "taskId": "A1", "type": "FINISH_START", "minLag": 0 }] }
 * }</pre>
 * Defaults applied here: {@code demand} = 1, {@code priority} = 0, {@code required} = TRUE, empty collections for
 * every list/set/map. {@code requirements} stays EMPTY when none is declared: the implicit single requirement
 * ({@link ResourceRequirement#implicit()}) is materialised by the normalizer (see {@link #effectiveRequirements()}),
 * after constraint rewrites such as {@code multi_resource_requirement}. {@code duration} stays null when omitted
 * (allowed in slot mode only). Range/reference checks belong to the request validator.
 *
 * @param id                unique, non-blank, at most 128 characters
 * @param typeId            optional, validated against {@code domain.taskTypes} when a domain is given
 * @param name              display only
 * @param duration          ticks {@code >= 0}; null allowed only in slot mode (slot length); 0 = milestone
 * @param resourceDurations resourceId to ticks overriding {@code duration}; only for tasks with exactly ONE
 *                          requirement of count 1
 * @param demand            amount consumed on each used resource, {@code >= 1}, default 1
 * @param priority          {@code >= 0}, default 0 (maximize_task_priority, tardiness weights, tie-breaks)
 * @param required          default TRUE; false = optional task (prize collecting)
 * @param prize             {@code >= 0}; reward when an OPTIONAL task is selected; null means 1
 * @param requirements      resource demands (empty = one implicit requirement)
 * @param release           earliest start, null = horizon start
 * @param deadline          HARD latest END ({@code end <= deadline}), null = horizon end
 * @param dueDate           SOFT target end for tardiness objectives, null = none
 * @param startSlots        slot ids or "ALL"; empty = free start
 * @param pin               hard pinned start and/or resources, null = free
 * @param predecessors      HARD edges "predecessor to this task"
 * @param setupClass        key into the setup matrix classes, null = none
 * @param tags              free grouping labels ("class-7A", "order-17") used by scopes, no_overlap, same_resource
 * @param preferences       soft preferences read by minimize_preference_penalty
 * @param attributes        opaque extension map, NEVER read by the engine
 */
public record Task(
        String id,
        String typeId,
        String name,
        Integer duration,
        Map<String, Integer> resourceDurations,
        Integer demand,
        Integer priority,
        Boolean required,
        Long prize,
        List<ResourceRequirement> requirements,
        Instant release,
        Instant deadline,
        Instant dueDate,
        List<String> startSlots,
        Pin pin,
        List<PrecedenceRef> predecessors,
        String setupClass,
        Set<String> tags,
        List<Preference> preferences,
        Map<String, Object> attributes) {

    /**
     * Maximum length of a task id.
     */
    public static final int ID_MAX_LENGTH = 128;

    public Task {
        resourceDurations = CollectionCopies.orderedMap(resourceDurations);
        demand = demand == null ? Integer.valueOf(1) : demand;
        priority = priority == null ? Integer.valueOf(0) : priority;
        required = required == null ? Boolean.TRUE : required;
        requirements = CollectionCopies.list(requirements);
        startSlots = CollectionCopies.list(startSlots);
        predecessors = CollectionCopies.list(predecessors);
        tags = CollectionCopies.orderedSet(tags);
        preferences = CollectionCopies.list(preferences);
        attributes = ImmutableJsonMap.copyOf(attributes);
    }

    /**
     * @return {@code true} unless {@code required} is explicitly false
     */
    public boolean requiredTask() {
        return !Boolean.FALSE.equals(required);
    }

    /**
     * @return the declared requirements, or the single implicit one when none is declared
     */
    public List<ResourceRequirement> effectiveRequirements() {
        return requirements.isEmpty() ? List.of(ResourceRequirement.implicit()) : requirements;
    }

    /**
     * @return the prize used when the task is optional and selected: {@code prize}, or 1 when null
     */
    public long effectivePrize() {
        return prize == null ? 1L : prize;
    }
}

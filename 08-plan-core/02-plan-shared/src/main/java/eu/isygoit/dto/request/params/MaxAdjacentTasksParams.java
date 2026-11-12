package eu.isygoit.dto.request.params;

/**
 * Maximum length of a run of directly adjacent tasks assigned to the same resource.
 *
 * @param max maximum number of adjacent tasks
 */
public record MaxAdjacentTasksParams(int max) {
}

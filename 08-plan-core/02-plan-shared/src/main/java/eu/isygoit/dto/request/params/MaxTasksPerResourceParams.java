package eu.isygoit.dto.request.params;

/**
 * Params of {@code max_tasks_per_resource}: number of tasks a resource takes ({@code <= max} and/or
 * {@code >= min}) over the whole horizon, or per period.
 * <pre>{@code
 * { "max": 3, "period": { "unit": "WEEK" } }
 * }</pre>
 *
 * @param max    upper bound, null = none
 * @param min    lower bound, null = none
 * @param period bucket definition, null = whole horizon
 */
public record MaxTasksPerResourceParams(Integer max, Integer min, PeriodSpec period) {
}

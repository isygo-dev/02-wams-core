package eu.isygoit.dto.request.params;

/**
 * Maximum number of scoped tasks that may start in one period.
 *
 * @param max    maximum task count per period
 * @param period period definition, normally DAY
 */
public record MaxTasksPerPeriodParams(int max, PeriodSpec period) {
}

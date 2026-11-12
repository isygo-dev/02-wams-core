package eu.isygoit.dto.request.params;

/**
 * Maximum number of periods in which a scoped resource may have work.
 *
 * @param max    maximum number of worked periods
 * @param period period definition, normally DAY
 */
public record MaxWorkedPeriodsParams(int max, PeriodSpec period) {
}

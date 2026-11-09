package eu.isygoit.dto.request.params;

/**
 * Params of {@code max_load_per_period}: per resource and period the load (ticks or task count) must be
 * {@code <= max} and/or {@code >= min}. A task counts in the period in which it STARTS. {@code metric} defaults to
 * {@link LoadMetric#TICKS}.
 * <pre>{@code
 * { "period": { "unit": "DAY" }, "metric": "TICKS", "max": 8 }
 * }</pre>
 *
 * @param period bucket definition
 * @param metric what is counted, default TICKS
 * @param max    upper bound, null = none
 * @param min    lower bound, null = none
 */
public record MaxLoadPerPeriodParams(PeriodSpec period, LoadMetric metric, Integer max, Integer min) {

    public MaxLoadPerPeriodParams {
        metric = metric == null ? LoadMetric.TICKS : metric;
    }
}

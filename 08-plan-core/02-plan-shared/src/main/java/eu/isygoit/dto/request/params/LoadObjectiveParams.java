package eu.isygoit.dto.request.params;

/**
 * Params of {@code minimize_max_load} and {@code balance_load}. {@code metric} defaults to
 * {@link LoadMetric#TICKS}.
 * <pre>{@code
 * { "metric": "COUNT" }
 * }</pre>
 *
 * @param metric what the load counts
 */
public record LoadObjectiveParams(LoadMetric metric) {

    public LoadObjectiveParams {
        metric = metric == null ? LoadMetric.TICKS : metric;
    }
}

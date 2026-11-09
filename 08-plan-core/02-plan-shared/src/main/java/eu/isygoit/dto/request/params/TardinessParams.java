package eu.isygoit.dto.request.params;

/**
 * Params of {@code minimize_total_tardiness}. {@code weightBy = PRIORITY} multiplies each tardiness by
 * {@code 1 + task.priority}; default {@link WeightBy#NONE}.
 * <pre>{@code
 * { "weightBy": "PRIORITY" }
 * }</pre>
 *
 * @param weightBy weighting of the tardiness of each task
 */
public record TardinessParams(WeightBy weightBy) {

    /**
     * Weighting modes.
     */
    public enum WeightBy { NONE, PRIORITY }

    public TardinessParams {
        weightBy = weightBy == null ? WeightBy.NONE : weightBy;
    }
}

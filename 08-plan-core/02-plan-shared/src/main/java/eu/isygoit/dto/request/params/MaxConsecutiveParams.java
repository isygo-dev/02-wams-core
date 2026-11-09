package eu.isygoit.dto.request.params;

/**
 * Params of {@code max_consecutive}: a resource works in at most {@code max} consecutive periods. Both components
 * are required ({@code max} is a primitive: omitting it is a binding error).
 * <pre>{@code
 * { "period": { "unit": "DAY" }, "max": 2 }
 * }</pre>
 *
 * @param period bucket definition
 * @param max    maximum number of consecutive periods with work
 */
public record MaxConsecutiveParams(PeriodSpec period, int max) {
}

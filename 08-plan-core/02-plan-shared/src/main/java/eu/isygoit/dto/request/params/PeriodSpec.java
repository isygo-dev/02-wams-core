package eu.isygoit.dto.request.params;

/**
 * How the horizon is cut into consecutive buckets ("periods") by windowed families. {@code TICKS} needs
 * {@code ticks >= 1}; {@code DAY} and {@code WEEK} are calendar-local in {@code meta.timezone} (a week starts on
 * Monday). Buckets tile the horizon; a task belongs to the bucket in which it STARTS.
 * <pre>{@code
 * { "unit": "DAY" }          { "unit": "TICKS", "ticks": 96 }
 * }</pre>
 *
 * @param unit  bucket unit
 * @param ticks bucket length in ticks (TICKS unit only)
 */
public record PeriodSpec(Unit unit, Integer ticks) {

    /**
     * Period units.
     */
    public enum Unit { TICKS, DAY, WEEK }
}

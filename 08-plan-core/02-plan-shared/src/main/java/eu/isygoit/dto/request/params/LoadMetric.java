package eu.isygoit.dto.request.params;

/**
 * What a "load" counts: the summed duration of the tasks of a resource ({@link #TICKS}) or their number
 * ({@link #COUNT}).
 */
public enum LoadMetric {
    /**
     * Sum of task durations in ticks.
     */
    TICKS,
    /**
     * Number of tasks.
     */
    COUNT
}

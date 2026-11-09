package eu.isygoit.solver.check;

import eu.isygoit.solver.spi.ViolationSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link ViolationSink} that records every reported violation instance in memory (design 8.2 step 3).
 * <p>
 * One collector per (constraint, validation); not thread-safe (used on the validating thread). The measure is in the
 * family's units (ticks, tasks, ...), exactly what the contributor penalises; {@link #totalMeasure()} is the sum and is
 * what the validator multiplies by the constraint weight.
 */
public final class ViolationCollector implements ViolationSink {

    /** One reported violation instance. {@code taskId}/{@code resourceId} may be {@code null}. */
    public record Entry(String taskId, String resourceId, String code, String message, long measure) {
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void violation(String taskId, String resourceId, String code, String message, long measure) {
        entries.add(new Entry(taskId, resourceId, code, message, measure));
    }

    /** @return the recorded entries in report order (unmodifiable) */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** @return {@code true} when no violation was reported */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** @return number of recorded instances */
    public int count() {
        return entries.size();
    }

    /** @return the sum of all measures, saturating at {@link Long#MAX_VALUE} */
    public long totalMeasure() {
        long sum = 0L;
        for (Entry entry : entries) {
            long next = sum + entry.measure();
            if (((sum ^ next) & (entry.measure() ^ next)) < 0) {
                return entry.measure() < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
            sum = next;
        }
        return sum;
    }
}

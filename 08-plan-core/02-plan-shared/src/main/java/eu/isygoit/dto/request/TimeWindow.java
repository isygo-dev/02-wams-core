package eu.isygoit.dto.request;

import java.time.Instant;

/**
 * A half-open time interval {@code [start, end)} expressed in instants. {@code start < end} is checked by the
 * request validator (code {@code CALENDAR_INVALID} or the owning field), not by this record.
 * <pre>{@code
 * { "start": "2026-09-07T08:00:00Z", "end": "2026-09-07T16:00:00Z" }
 * }</pre>
 *
 * @param start inclusive start
 * @param end   exclusive end
 */
public record TimeWindow(Instant start, Instant end) {
}

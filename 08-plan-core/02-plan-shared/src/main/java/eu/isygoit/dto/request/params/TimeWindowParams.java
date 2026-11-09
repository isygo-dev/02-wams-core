package eu.isygoit.dto.request.params;

import java.time.Instant;

/**
 * Params of {@code time_window}: extra windows on the scoped tasks. Every bound is optional; given bounds must be
 * tick aligned.
 * <pre>{@code
 * { "earliestStart": "2026-09-07T09:00:00Z", "latestEnd": "2026-09-07T17:00:00Z" }
 * }</pre>
 *
 * @param earliestStart start must be at or after this instant
 * @param latestStart   start must be at or before this instant
 * @param earliestEnd   end must be at or after this instant
 * @param latestEnd     end must be at or before this instant
 */
public record TimeWindowParams(Instant earliestStart, Instant latestStart, Instant earliestEnd, Instant latestEnd) {
}

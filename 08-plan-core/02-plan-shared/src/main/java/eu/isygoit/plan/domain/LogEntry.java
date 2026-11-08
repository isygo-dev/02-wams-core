package eu.isygoit.plan.domain;

import eu.isygoit.enums.IEnumLogLevel;

import java.time.Instant;
import java.util.UUID;

public record LogEntry(
        UUID jobId,
        String tenant,
        Instant bucketStart,
        Instant eventTime,
        UUID eventId,
        IEnumLogLevel.Types level,
        String step,
        String engine,
        String message,
        String details,
        long durationMillis,
        String traceId,
        String spanId) {
}

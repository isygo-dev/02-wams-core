package eu.isygoit.plan.domain;

import eu.isygoit.enums.IEnumJobStatusCode;

import java.time.Instant;
import java.util.UUID;

public record JobStatus(
        UUID jobId,
        IEnumJobStatusCode.Types status,
        Instant submittedAt,
        Instant startedAt,
        Instant finishedAt,
        String errorCode,
        String message) {
}

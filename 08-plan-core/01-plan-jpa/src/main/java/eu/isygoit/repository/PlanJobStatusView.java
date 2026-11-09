package eu.isygoit.repository;

import eu.isygoit.enums.IEnumJobStatusCode;

import java.time.Instant;

/**
 * Closed interface projection of {@link eu.isygoit.model.PlanJob}: only the lightweight columns,
 * never the large jsonb payloads (request, result, diagnostics).
 */
public interface PlanJobStatusView {
    Long getId();

    String getCode();

    String getTenant();

    String getClientRef();

    String getEngine();

    IEnumJobStatusCode.Types getStatus();

    Instant getSubmittedAt();

    Instant getStartedAt();

    Instant getCompletedAt();

    String getErrorCode();

    String getMessage();
}

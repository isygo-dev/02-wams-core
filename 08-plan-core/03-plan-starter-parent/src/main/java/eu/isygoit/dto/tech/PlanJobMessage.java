package eu.isygoit.dto.tech;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Kafka message published on the plan-jobs topic: a small pointer to a persisted planning job. The planning request
 * itself is never put on the wire; the worker loads it from the database by {@code jobId}.
 * <p>
 * Wire format: JSON object {@code {"jobId":123,"tenant":"acme"}}; the record key is {@code jobId} as a UTF-8 string
 * so a job is always routed to the same partition (and the id can be recovered from the key when the body is
 * unreadable, see the dead-letter handler).
 *
 * @param jobId  the database id (sequence) of the plan job, required
 * @param tenant the tenant owning the job (informational, may be null)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanJobMessage(Long jobId, String tenant) {
}

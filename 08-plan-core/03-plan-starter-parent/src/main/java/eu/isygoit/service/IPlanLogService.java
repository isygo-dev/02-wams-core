package eu.isygoit.service;

import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.enums.IEnumLogLevel;

import java.time.Instant;
import java.util.List;

/**
 * Execution log of the planning jobs (Cassandra, partitioned by tenant, job and 6-hour bucket, 30-day retention).
 */
public interface IPlanLogService {

    /**
     * Stores one log entry. The retention (30 days, {@code PlanLog.LOG_TTL_SECONDS}) is applied on write as the
     * Cassandra TTL of the row.
     */
    void append(LogEntry entry);

    /**
     * Log entries of one job, newest first.
     *
     * @param tenant   the tenant owning the job
     * @param jobId    the job id
     * @param from     start of the event-time range (inclusive); {@code null} together with {@code to} means the
     *                 whole retention window
     * @param to       end of the event-time range (inclusive)
     * @param minLevel keep only entries of this level or above; {@code null} keeps all
     * @param limit    maximum number of entries (at least 1)
     */
    List<LogEntry> findLatest(String tenant, Long jobId, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit);
}

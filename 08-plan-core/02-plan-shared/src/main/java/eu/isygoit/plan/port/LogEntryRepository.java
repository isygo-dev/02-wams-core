package eu.isygoit.plan.port;

import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.plan.domain.LogEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LogEntryRepository {
    void append(LogEntry entry);

    List<LogEntry> findByJobId(UUID jobId, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit);

    List<LogEntry> findLatest(UUID jobId, int limit);
}

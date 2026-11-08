package eu.isygoit.service.impl;

import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.model.PlanLog;
import eu.isygoit.model.PlanLogKey;
import eu.isygoit.plan.domain.LogBucket;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.plan.port.LogEntryRepository;
import eu.isygoit.repository.PlanLogRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Repository
public class CassandraLogEntryRepositoryAdapter implements LogEntryRepository {
    private static final int RETENTION_DAYS = 30;
    private static final int HOURS_PER_BUCKET = 6;

    private final PlanLogRepository repository;

    public CassandraLogEntryRepositoryAdapter(PlanLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(LogEntry entry) {
        repository.save(toEntity(entry));
    }

    @Override
    public List<LogEntry> findByJobId(
            UUID jobId, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit) {
        if (from == null || to == null || from.isAfter(to) || limit < 1) {
            throw new IllegalArgumentException("A valid time range and positive limit are required");
        }
        Instant now = Instant.now();
        Instant retainedFrom = now.minus(RETENTION_DAYS, ChronoUnit.DAYS);
        Instant effectiveFrom = from.isBefore(retainedFrom) ? retainedFrom : from;
        Instant effectiveTo = to.isAfter(now) ? now : to;
        if (effectiveFrom.isAfter(effectiveTo)) {
            return List.of();
        }
        return readBuckets(jobId, effectiveFrom, effectiveTo).stream()
                .filter(entry -> !entry.eventTime().isBefore(effectiveFrom)
                        && !entry.eventTime().isAfter(effectiveTo))
                .filter(entry -> minLevel == null || entry.level().ordinal() >= minLevel.ordinal())
                .sorted(Comparator.comparing(LogEntry::eventTime).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<LogEntry> findLatest(UUID jobId, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("A positive limit is required");
        }
        Instant to = Instant.now();
        return findByJobId(jobId, to.minus(RETENTION_DAYS, ChronoUnit.DAYS), to, null, limit);
    }

    private List<LogEntry> readBuckets(UUID jobId, Instant from, Instant to) {
        Instant firstBucket = LogBucket.start(from);
        Instant lastBucket = LogBucket.start(to);
        List<LogEntry> entries = new ArrayList<>();
        for (Instant bucket = firstBucket; !bucket.isAfter(lastBucket);
             bucket = bucket.plus(HOURS_PER_BUCKET, ChronoUnit.HOURS)) {
            repository.findByJobAndBucket(jobId, bucket).stream()
                    .map(this::toDomain)
                    .forEach(entries::add);
        }
        return entries;
    }

    private PlanLog toEntity(LogEntry entry) {
        return new PlanLog(
                new PlanLogKey(entry.jobId(), entry.tenant(), entry.bucketStart(), entry.eventTime(), entry.eventId()),
                entry.level(),
                entry.step(),
                entry.engine(),
                entry.message(),
                entry.details(),
                entry.durationMillis(),
                entry.traceId(),
                entry.spanId());
    }

    private LogEntry toDomain(PlanLog entity) {
        PlanLogKey key = entity.getId();
        return new LogEntry(key.getJobId(), key.getTenant(), key.getBucketStart(), key.getEventTime(), key.getEventId(),
                entity.getLevel(), entity.getStep(), entity.getEngine(), entity.getMessage(),
                entity.getDetails(), entity.getDurationMillis(), entity.getTraceId(), entity.getSpanId());
    }
}

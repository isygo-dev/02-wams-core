package eu.isygoit.service.impl;

import eu.isygoit.dto.data.LogBucket;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.model.PlanLog;
import eu.isygoit.model.PlanLogKey;
import eu.isygoit.repository.PlanLogRepository;
import eu.isygoit.service.IPlanLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Cassandra implementation of {@link IPlanLogService}.
 * <p>
 * Writes go through the {@link CassandraTemplate} so the 30-day retention can be applied as the row TTL
 * ({@link PlanLog#LOG_TTL_SECONDS}); nothing ever deletes log rows explicitly (deleting a job leaves its logs to
 * expire). Reads walk the 6-hour partitions of {@link PlanLogRepository} from the newest to the oldest.
 */
@Slf4j
@Service
public class PlanLogService implements IPlanLogService {
    private static final int RETENTION_DAYS = 30;
    private static final int HOURS_PER_BUCKET = 6;
    /**
     * Upper bound of rows read from one partition when a level filter is applied in memory.
     */
    private static final int MAX_ROWS_PER_PARTITION = 5000;

    private static final InsertOptions TTL_OPTIONS = InsertOptions.builder()
            .ttl(Duration.ofSeconds(PlanLog.LOG_TTL_SECONDS))
            .build();

    private final PlanLogRepository repository;
    private final CassandraTemplate cassandraTemplate;

    /**
     * @param repository        the partition reads
     * @param cassandraTemplate the TTL writes
     */
    public PlanLogService(PlanLogRepository repository, CassandraTemplate cassandraTemplate) {
        this.repository = repository;
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public void append(LogEntry entry) {
        cassandraTemplate.insert(toEntity(entry), TTL_OPTIONS);
    }

    @Override
    public List<LogEntry> findLatest(
            String tenant, Long jobId, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit) {
        if (tenant == null || tenant.isBlank() || limit < 1) {
            throw new IllegalArgumentException("A tenant and positive limit are required");
        }
        Instant now = Instant.now();
        Instant retainedFrom = now.minus(RETENTION_DAYS, ChronoUnit.DAYS);
        Instant rangeFrom = from == null ? retainedFrom : from;
        Instant rangeTo = to == null ? now : to;
        if (rangeFrom.isAfter(rangeTo)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        Instant effectiveFrom = rangeFrom.isBefore(retainedFrom) ? retainedFrom : rangeFrom;
        Instant effectiveTo = rangeTo.isAfter(now) ? now : rangeTo;
        if (effectiveFrom.isAfter(effectiveTo)) {
            return List.of();
        }
        return readBucketsNewestFirst(tenant, jobId, effectiveFrom, effectiveTo, minLevel, limit);
    }

    /**
     * Walks the 6-hour partitions from the newest to the oldest and stops as soon as {@code limit}
     * entries are collected. Each partition is clustered newest first, so the concatenation is
     * already ordered newest first. Without a level filter at most the still-missing number of rows is
     * read per partition; with a level filter the rows are filtered in memory after the fetch, so a
     * bounded larger page is read per partition.
     */
    private List<LogEntry> readBucketsNewestFirst(
            String tenant, Long jobId, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit) {
        Instant firstBucket = LogBucket.start(from);
        List<LogEntry> entries = new ArrayList<>(Math.min(limit, MAX_ROWS_PER_PARTITION));
        for (Instant bucket = LogBucket.start(to);
             !bucket.isBefore(firstBucket) && entries.size() < limit;
             bucket = bucket.minus(HOURS_PER_BUCKET, ChronoUnit.HOURS)) {
            int remaining = limit - entries.size();
            int fetchSize = minLevel == null ? remaining : MAX_ROWS_PER_PARTITION;
            repository.findNewestInBucket(tenant, jobId, bucket, from, to, fetchSize).stream()
                    .map(this::toDomain)
                    .filter(entry -> minLevel == null
                            || (entry.level() != null && entry.level().ordinal() >= minLevel.ordinal()))
                    .limit(remaining)
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

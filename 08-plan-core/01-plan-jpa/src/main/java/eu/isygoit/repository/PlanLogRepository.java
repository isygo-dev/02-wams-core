package eu.isygoit.repository;

import eu.isygoit.annotation.IgnoreRepository;
import eu.isygoit.model.PlanLog;
import eu.isygoit.model.PlanLogKey;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.time.Instant;
import java.util.List;

@IgnoreRepository
public interface PlanLogRepository extends CassandraRepository<PlanLog, PlanLogKey> {
    /**
     * Reads at most {@code limit} rows of ONE partition (tenant, job, bucket), restricted to the
     * event-time range. The table clustering order is (event_time DESC, event_id DESC), so rows come
     * back newest first without any in-memory sort.
     */
    @Query("SELECT * FROM " + SchemaTableConstantName.T_PLAN_LOG_ENTRIES
            + " WHERE " + SchemaColumnConstantName.C_TENANT + " = ?0 AND "
            + SchemaColumnConstantName.C_JOB_ID + " = ?1 AND "
            + SchemaColumnConstantName.C_BUCKET_START + " = ?2 AND "
            + SchemaColumnConstantName.C_EVENT_TIME + " >= ?3 AND "
            + SchemaColumnConstantName.C_EVENT_TIME + " <= ?4 LIMIT ?5")
    List<PlanLog> findNewestInBucket(
            String tenant, Long jobId, Instant bucketStart, Instant from, Instant to, int limit);
}

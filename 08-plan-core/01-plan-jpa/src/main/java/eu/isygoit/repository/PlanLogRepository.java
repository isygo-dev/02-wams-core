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
import java.util.UUID;

@IgnoreRepository
public interface PlanLogRepository extends CassandraRepository<PlanLog, PlanLogKey> {
    @Query("SELECT * FROM " + SchemaTableConstantName.T_PLAN_LOG_ENTRIES
            + " WHERE " + SchemaColumnConstantName.C_JOB_ID + " = ?0 AND "
            + SchemaColumnConstantName.C_BUCKET_START + " = ?1")
    List<PlanLog> findByJobAndBucket(UUID jobId, Instant bucketStart);
}

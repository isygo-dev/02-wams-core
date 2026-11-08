package eu.isygoit.repository;

import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.model.PlanJob;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlanJobRepository extends JpaPagingAndSortingTenantAssignableRepository<PlanJob, UUID> {
    List<PlanJob> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    long countByStatus(IEnumJobStatusCode.Types status);

    @Query(value = "select cast(" + SchemaColumnConstantName.C_REQUEST_JSON + " as text)"
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "and " + SchemaColumnConstantName.C_REQUEST_JSON + " is not null "
            + "order by " + SchemaColumnConstantName.C_SUBMITTED_AT + " asc",
            nativeQuery = true)
    List<String> findCreatedRequestJson(Pageable pageable);

    @Modifying
    @Query(value = "insert into " + SchemaTableConstantName.T_PLAN_JOB
            + " (" + SchemaColumnConstantName.C_JOB_ID
            + ", " + SchemaColumnConstantName.C_STATUS
            + ", " + SchemaColumnConstantName.C_SUBMITTED_AT
            + ", " + SchemaColumnConstantName.C_MESSAGE
            + ", " + SchemaColumnConstantName.C_REQUEST_JSON
            + ", " + SchemaColumnConstantName.C_VERSION + ") "
            + "values (:jobId, 'CREATED', :submittedAt, :message, cast(:requestJson as jsonb), 0) "
            + "on conflict (" + SchemaColumnConstantName.C_JOB_ID + ") do nothing",
            nativeQuery = true)
    int insertCreatedIfAbsent(
            @Param("jobId") UUID jobId,
            @Param("submittedAt") Instant submittedAt,
            @Param("message") String message,
            @Param("requestJson") String requestJson);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'NORMALIZING', "
            + SchemaColumnConstantName.C_STARTED_AT + " = :startedAt, "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Normalizing request', "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int claimForProcessing(@Param("jobId") UUID jobId, @Param("startedAt") Instant startedAt);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'CANCELLED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Cancelled', "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int cancelIfRunning(@Param("jobId") UUID jobId);
}

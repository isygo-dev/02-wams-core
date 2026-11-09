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

    @Query(value = "select cast(" + SchemaColumnConstantName.C_REQUEST_JSON + " as text)"
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId",
            nativeQuery = true)
    String findRequestJson(@Param("jobId") UUID jobId);

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
            + " set " + SchemaColumnConstantName.C_REQUEST_JSON + " = cast(:requestJson as jsonb), "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Job updated', "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int updateCreatedRequest(@Param("jobId") UUID jobId, @Param("requestJson") String requestJson);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING', "
            + SchemaColumnConstantName.C_MESSAGE + " = :message, "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'NORMALIZING'",
            nativeQuery = true)
    int markSolving(@Param("jobId") UUID jobId, @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = :status, "
            + SchemaColumnConstantName.C_RESULT_JSON + " = cast(:resultJson as jsonb), "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = :finishedAt, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = :errorCode, "
            + SchemaColumnConstantName.C_MESSAGE + " = :message, "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING'",
            nativeQuery = true)
    int completeSolving(
            @Param("jobId") UUID jobId,
            @Param("status") String status,
            @Param("resultJson") String resultJson,
            @Param("finishedAt") Instant finishedAt,
            @Param("errorCode") String errorCode,
            @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'FAILED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = :errorCode, "
            + SchemaColumnConstantName.C_MESSAGE + " = :message, "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING'",
            nativeQuery = true)
    int markFailedIfSolving(
            @Param("jobId") UUID jobId,
            @Param("errorCode") String errorCode,
            @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'INFEASIBLE', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = 'INFEASIBLE', "
            + SchemaColumnConstantName.C_MESSAGE + " = :message, "
            + SchemaColumnConstantName.C_DIAGNOSTICS_JSON + " = cast(:diagnostics as jsonb), "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING'",
            nativeQuery = true)
    int markInfeasibleIfSolving(
            @Param("jobId") UUID jobId,
            @Param("message") String message,
            @Param("diagnostics") String diagnostics);

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

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING', "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Stop requested', "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " in ('NORMALIZING', 'SOLVING')",
            nativeQuery = true)
    int stopIfProcessing(@Param("jobId") UUID jobId);

    @Query(value = "select " + SchemaColumnConstantName.C_JOB_ID
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING' "
            + "order by " + SchemaColumnConstantName.C_SUBMITTED_AT + " asc",
            nativeQuery = true)
    List<UUID> findStoppingJobIds(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'CANCELLED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Cancelled', "
            + SchemaColumnConstantName.C_VERSION + " = " + SchemaColumnConstantName.C_VERSION + " + 1 "
            + "where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING'",
            nativeQuery = true)
    int completeStopped(@Param("jobId") UUID jobId);

    @Modifying(clearAutomatically = true)
    @Query(value = "delete from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_JOB_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS
            + " in ('SUCCEEDED', 'INFEASIBLE', 'FAILED', 'CANCELLED')",
            nativeQuery = true)
    int deleteIfTerminal(@Param("jobId") UUID jobId);
}

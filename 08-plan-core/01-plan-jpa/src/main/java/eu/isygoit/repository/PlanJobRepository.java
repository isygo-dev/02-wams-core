package eu.isygoit.repository;

import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.model.PlanJob;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlanJobRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<PlanJob, Long> {
    List<PlanJob> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    /**
     * Lightweight status lookup (no jsonb payload columns).
     */
    Optional<PlanJobStatusView> findStatusViewById(Long id);

    /**
     * Lightweight recent-jobs listing (no jsonb payload columns), newest first.
     */
    List<PlanJobStatusView> findStatusViewsByOrderBySubmittedAtDesc(Pageable pageable);

    long countByStatus(IEnumJobStatusCode.Types status);

    /**
     * Tenant-scoped variant of {@link #countGroupedByStatus()} (uses idx_plan_job_tenant_submitted).
     */
    @Query("select j.status as status, count(j) as total from PlanJob j where j.tenant = :tenant group by j.status")
    List<PlanJobStatusCount> countGroupedByStatusForTenant(@Param("tenant") String tenant);

    /**
     * Lightweight recent-jobs listing of one tenant (no jsonb payload columns), newest first.
     */
    List<PlanJobStatusView> findStatusViewsByTenantOrderBySubmittedAtDesc(String tenant, Pageable pageable);

    /**
     * Records the dispatch made by the submitting instance (first dispatch only).
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_DISPATCHED_AT + " = :dispatchedAt, "
            + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " = " + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " + 1, "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "and " + SchemaColumnConstantName.C_DISPATCHED_AT + " is null",
            nativeQuery = true)
    int markDispatched(@Param("jobId") Long jobId, @Param("dispatchedAt") Instant dispatchedAt);

    /**
     * Atomic batch claim: one UPDATE ... RETURNING over a row-locked (skip locked) selection, so concurrent
     * instances never claim the same job.
     */
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_DISPATCHED_AT + " = :now, "
            + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " = " + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " + 1, "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " in (select " + SchemaColumnConstantName.C_ID
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "and " + SchemaColumnConstantName.C_REQUEST_JSON + " is not null "
            + "and (" + SchemaColumnConstantName.C_DISPATCHED_AT + " is null or "
            + SchemaColumnConstantName.C_DISPATCHED_AT + " < :staleBefore) "
            + "and " + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " < :maxAttempts "
            + "order by " + SchemaColumnConstantName.C_SUBMITTED_AT + " asc "
            + "limit :limit for update skip locked) "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "returning " + SchemaColumnConstantName.C_ID,
            nativeQuery = true)
    List<Long> claimForDispatch(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("maxAttempts") int maxAttempts,
            @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_DISPATCHED_AT + " = null, "
            + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " = case when :refund then greatest("
            + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " - 1, 0) else "
            + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " end, "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int releaseDispatchClaim(@Param("jobId") Long jobId, @Param("refund") boolean refund);

    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'FAILED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = " + PlanJobSql.ERROR_CODE_PARAM + ", "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " in (select " + SchemaColumnConstantName.C_ID
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "and " + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS + " >= :maxAttempts "
            + "and (" + SchemaColumnConstantName.C_DISPATCHED_AT + " is null or "
            + SchemaColumnConstantName.C_DISPATCHED_AT + " < :staleBefore) "
            + "order by " + SchemaColumnConstantName.C_SUBMITTED_AT + " asc "
            + "limit :limit for update skip locked) "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "returning " + SchemaColumnConstantName.C_ID,
            nativeQuery = true)
    List<Long> failExhaustedDispatch(
            @Param("staleBefore") Instant staleBefore,
            @Param("maxAttempts") int maxAttempts,
            @Param("errorCode") String errorCode,
            @Param("message") String message,
            @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'CREATED', "
            + SchemaColumnConstantName.C_STARTED_AT + " = null, "
            + SchemaColumnConstantName.C_DISPATCHED_AT + " = null, "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " in ('NORMALIZING', 'SOLVING')",
            nativeQuery = true)
    int releaseClaim(@Param("jobId") Long jobId, @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'FAILED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = " + PlanJobSql.ERROR_CODE_PARAM + ", "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " in ('CREATED', 'NORMALIZING', 'SOLVING', 'STOPPING')",
            nativeQuery = true)
    int failIfNotTerminal(
            @Param("jobId") Long jobId,
            @Param("errorCode") String errorCode,
            @Param("message") String message);

    @Query(value = "select " + SchemaColumnConstantName.C_ID
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING' "
            + "and " + SchemaColumnConstantName.C_UPDATE_DATE + " < localtimestamp - :graceSeconds * interval '1 second' "
            + "order by " + SchemaColumnConstantName.C_UPDATE_DATE + " asc",
            nativeQuery = true)
    List<Long> findExpiredStoppingJobIds(@Param("graceSeconds") int graceSeconds, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'CANCELLED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING' "
            + "and " + SchemaColumnConstantName.C_UPDATE_DATE + " < localtimestamp - :graceSeconds * interval '1 second'",
            nativeQuery = true)
    int cancelExpiredStopping(
            @Param("jobId") Long jobId,
            @Param("graceSeconds") int graceSeconds,
            @Param("message") String message);

    /**
     * One grouped query replacing one count query per status.
     */
    @Query("select j.status as status, count(j) as total from PlanJob j group by j.status")
    List<PlanJobStatusCount> countGroupedByStatus();

    @Query(value = "select cast(" + SchemaColumnConstantName.C_REQUEST_JSON + " as text)"
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'CREATED' "
            + "and " + SchemaColumnConstantName.C_REQUEST_JSON + " is not null "
            + "order by " + SchemaColumnConstantName.C_SUBMITTED_AT + " asc",
            nativeQuery = true)
    List<String> findCreatedRequestJson(Pageable pageable);

    @Query(value = "select " + SchemaColumnConstantName.C_ID
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " in ('NORMALIZING', 'SOLVING') "
            + "and " + SchemaColumnConstantName.C_STARTED_AT + " is not null "
            + "and " + SchemaColumnConstantName.C_STARTED_AT + " < current_timestamp - ("
            + "coalesce(cast(nullif("
            + SchemaColumnConstantName.C_REQUEST_JSON + " -> 'solverOptions' ->> 'maxTimeSeconds', '') "
            + "as integer), 30) + :graceSeconds) * interval '1 second' "
            + "order by " + SchemaColumnConstantName.C_STARTED_AT + " asc",
            nativeQuery = true)
    List<Long> findExpiredProcessingJobIds(
            @Param("graceSeconds") int graceSeconds,
            Pageable pageable);

    @Query(value = "select cast(" + SchemaColumnConstantName.C_REQUEST_JSON + " as text)"
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_ID + " = :jobId",
            nativeQuery = true)
    String findRequestJson(@Param("jobId") Long jobId);

    @Query(value = "select coalesce("
            + SchemaColumnConstantName.C_REQUEST_JSON + " ->> 'tenant', "
            + SchemaColumnConstantName.C_TENANT + ")"
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_ID + " = :jobId",
            nativeQuery = true)
    Optional<String> findTenantByJobId(@Param("jobId") Long jobId);

    /**
     * Idempotent insert of a CREATED job. The primary key comes from {@code plan_job_sequence} (same sequence as the
     * JPA generator, allocationSize 1). A job whose (tenant, client_ref) already exists is NOT inserted.
     * (A native statement is kept instead of JPA save so the idempotency race is resolved atomically by PostgreSQL.)
     *
     * @return the new job id, or empty when the (tenant, client_ref) pair already exists
     */
    @Query(value = "insert into " + SchemaTableConstantName.T_PLAN_JOB
            + " (" + SchemaColumnConstantName.C_ID
            + ", " + SchemaColumnConstantName.C_TENANT
            + ", " + SchemaColumnConstantName.C_CODE
            + ", " + SchemaColumnConstantName.C_CLIENT_REF
            + ", " + SchemaColumnConstantName.C_ENGINE
            + ", " + SchemaColumnConstantName.C_STATUS
            + ", " + SchemaColumnConstantName.C_SUBMITTED_AT
            + ", " + SchemaColumnConstantName.C_MESSAGE
            + ", " + SchemaColumnConstantName.C_REQUEST_JSON
            + ", " + SchemaColumnConstantName.C_DISPATCH_ATTEMPTS
            + ", " + SchemaColumnConstantName.C_VERSION
            + ", " + SchemaColumnConstantName.C_CREATE_DATE
            + ", " + SchemaColumnConstantName.C_CREATED_BY
            + ", " + SchemaColumnConstantName.C_UPDATE_DATE
            + ", " + SchemaColumnConstantName.C_UPDATED_BY + ") "
            + "values (nextval('" + PlanJob.SEQUENCE_NAME + "'), :tenant, :code, cast(:clientRef as text), cast(:engine as text), 'CREATED', "
            + ":submittedAt, " + PlanJobSql.MESSAGE_PARAM + ", "
            + "cast(:requestJson as jsonb), 0, 0, localtimestamp, :actor, localtimestamp, :actor) "
            + "on conflict (" + SchemaColumnConstantName.C_TENANT + ", " + SchemaColumnConstantName.C_CLIENT_REF
            + ") do nothing "
            + "returning " + SchemaColumnConstantName.C_ID,
            nativeQuery = true)
    Optional<Long> insertCreatedIfAbsent(
            @Param("tenant") String tenant,
            @Param("code") String code,
            @Param("clientRef") String clientRef,
            @Param("engine") String engine,
            @Param("submittedAt") Instant submittedAt,
            @Param("message") String message,
            @Param("requestJson") String requestJson,
            @Param("actor") String actor);

    /**
     * Idempotency lookup by the client reference (case-sensitive, as the unique constraint).
     */
    Optional<PlanJob> findByTenantAndClientRef(String tenant, String clientRef);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'NORMALIZING', "
            + SchemaColumnConstantName.C_STARTED_AT + " = :startedAt, "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Normalizing request', "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int claimForProcessing(@Param("jobId") Long jobId, @Param("startedAt") Instant startedAt);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_REQUEST_JSON + " = cast(:requestJson as jsonb), "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Job updated', "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int updateCreatedRequest(@Param("jobId") Long jobId, @Param("requestJson") String requestJson);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING', "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'NORMALIZING'",
            nativeQuery = true)
    int markSolving(@Param("jobId") Long jobId, @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = :status, "
            + SchemaColumnConstantName.C_RESULT_JSON + " = cast(:resultJson as jsonb), "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = :finishedAt, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = " + PlanJobSql.ERROR_CODE_PARAM + ", "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING'",
            nativeQuery = true)
    int completeSolving(
            @Param("jobId") Long jobId,
            @Param("status") String status,
            @Param("resultJson") String resultJson,
            @Param("finishedAt") Instant finishedAt,
            @Param("errorCode") String errorCode,
            @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'FAILED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = " + PlanJobSql.ERROR_CODE_PARAM + ", "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING'",
            nativeQuery = true)
    int markFailedIfSolving(
            @Param("jobId") Long jobId,
            @Param("errorCode") String errorCode,
            @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'FAILED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = " + PlanJobSql.ERROR_CODE_PARAM + ", "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " in ('NORMALIZING', 'SOLVING') "
            + "and " + SchemaColumnConstantName.C_STARTED_AT + " is not null "
            + "and " + SchemaColumnConstantName.C_STARTED_AT + " < current_timestamp - ("
            + "coalesce(cast(nullif("
            + SchemaColumnConstantName.C_REQUEST_JSON + " -> 'solverOptions' ->> 'maxTimeSeconds', '') "
            + "as integer), 30) + :graceSeconds) * interval '1 second'",
            nativeQuery = true)
    int failExpiredProcessing(
            @Param("jobId") Long jobId,
            @Param("graceSeconds") int graceSeconds,
            @Param("errorCode") String errorCode,
            @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'INFEASIBLE', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_ERROR_CODE + " = 'INFEASIBLE', "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + SchemaColumnConstantName.C_DIAGNOSTICS_JSON + " = cast(:diagnostics as jsonb), "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'SOLVING'",
            nativeQuery = true)
    int markInfeasibleIfSolving(
            @Param("jobId") Long jobId,
            @Param("message") String message,
            @Param("diagnostics") String diagnostics);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'CANCELLED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Cancelled', "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'CREATED'",
            nativeQuery = true)
    int cancelIfRunning(@Param("jobId") Long jobId);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING', "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Stop requested', "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " in ('NORMALIZING', 'SOLVING')",
            nativeQuery = true)
    int stopIfProcessing(@Param("jobId") Long jobId);

    @Query(value = "select " + SchemaColumnConstantName.C_ID
            + " from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING' "
            + "order by " + SchemaColumnConstantName.C_SUBMITTED_AT + " asc",
            nativeQuery = true)
    List<Long> findStoppingJobIds(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = 'CANCELLED', "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = current_timestamp, "
            + SchemaColumnConstantName.C_MESSAGE + " = 'Cancelled', "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = 'STOPPING'",
            nativeQuery = true)
    int completeStopped(@Param("jobId") Long jobId);

    /**
     * Finishes a stopped job (STOPPING -> CANCELLED) while keeping the best solution found before the stop
     * (solver status STOPPED_FEASIBLE): the incumbent result is stored in the result column.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "update " + SchemaTableConstantName.T_PLAN_JOB
            + " set " + SchemaColumnConstantName.C_STATUS + " = CANCELLED, "
            + SchemaColumnConstantName.C_RESULT_JSON + " = cast(:resultJson as jsonb), "
            + SchemaColumnConstantName.C_COMPLETED_AT + " = :finishedAt, "
            + SchemaColumnConstantName.C_MESSAGE + " = " + PlanJobSql.MESSAGE_PARAM + ", "
            + PlanJobSql.SET_AUDIT_AND_VERSION
            + "where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS + " = STOPPING",
            nativeQuery = true)
    int completeStoppedWithResult(
            @Param("jobId") Long jobId,
            @Param("resultJson") String resultJson,
            @Param("finishedAt") Instant finishedAt,
            @Param("message") String message);

    @Modifying(clearAutomatically = true)
    @Query(value = "delete from " + SchemaTableConstantName.T_PLAN_JOB
            + " where " + SchemaColumnConstantName.C_ID + " = :jobId "
            + "and " + SchemaColumnConstantName.C_STATUS
            + " in ('SUCCEEDED', 'INFEASIBLE', 'FAILED', 'CANCELLED')",
            nativeQuery = true)
    int deleteIfTerminal(@Param("jobId") Long jobId);
}

package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.CapabilitiesDto;
import eu.isygoit.dto.response.JobDashboardSummary;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.model.PlanJob;

import java.time.Instant;
import java.util.List;

/**
 * Service of {@link PlanJob}: the stack tenant CRUD plus the planning specific operations.
 * <p>
 * The tenant is always passed in by the caller (the controller takes it from the authenticated request context);
 * every operation is scoped to it and a job of another tenant behaves exactly like a missing job. Deleting through
 * the inherited {@code delete} is only possible for finished jobs; creating through the inherited {@code create} is
 * not the way to create a job, use {@link #submit}.
 */
public interface IPlanJobService extends ICrudTenantServiceOperations<Long, PlanJob> {

    /**
     * Validates the request, scopes it to the caller tenant, generates the job code, stores a CREATED job and dispatches
     * it. Idempotent on {@code (tenant, request.meta().clientRef())} when the reference is present: the existing
     * job is returned and nothing is created.
     *
     * @throws eu.isygoit.exception.InvalidRequestException when the request is invalid or the caller tenant is missing
     * @throws eu.isygoit.exception.JobQueueException       when the job was stored but could not be dispatched
     *                                                      (the recovery sweep retries it)
     */
    PlanJob submit(String tenant, PlanningRequest request);

    /**
     * The job of the tenant.
     *
     * @throws eu.isygoit.exception.JobNotFoundException when missing or owned by another tenant
     */
    PlanJob getOwned(String tenant, Long id);

    /**
     * The stored planning request of the job, whatever its status.
     */
    PlanningRequest getRequest(String tenant, Long id);

    /**
     * Replaces the request of a job; only while the job is CREATED.
     *
     * @throws eu.isygoit.exception.ConflictException (409) when the job is no longer CREATED
     */
    PlanJob updateRequest(String tenant, Long id, PlanningRequest request);

    /**
     * The solve result: the stored result, or the reconstructed one of an INFEASIBLE job (no solution plus the
     * stored diagnostics).
     *
     * @throws eu.isygoit.exception.JobNotFoundException when the job does not exist or has no result yet
     */
    SolveResult getResult(String tenant, Long id);

    /**
     * Exports the result as a file.
     *
     * @param format  {@code json} (pretty JSON of the FULL result, the section is ignored) or {@code csv}
     * @param section the CSV section, {@code null} or blank = {@code assignments}. Sections and headers:
     *                <ul>
     *                <li>{@code assignments}: {@code taskId,optional,role,resourceId,amount,slotId,startTick,endTick,start,end}
     *                (one row per task and resource)</li>
     *                <li>{@code resources}: {@code resourceId,taskCount,loadTicks,firstStartTick,lastEndTick}</li>
     *                <li>{@code unassigned}: {@code taskId,reasonCode,forgonePrize,reason}</li>
     *                <li>{@code selected}: {@code taskId,startTick,endTick,start,end} (selected optional tasks)</li>
     *                <li>{@code violations}: {@code kind,constraintId,taskId,resourceId,code,impact,message}
     *                (kind = SOFT | RELAXED)</li>
     *                <li>{@code relaxations}: {@code constraintId,originalValue,relaxedValue,authorized,violationCount,measure,impactOnScore}</li>
     *                <li>{@code core}: {@code kind,id,reasonCode,message}</li>
     *                <li>{@code metrics}: {@code name,value}</li>
     *                </ul>
     * @throws eu.isygoit.exception.InvalidRequestException for an unknown format or section
     */
    byte[] exportResult(String tenant, Long id, String format, String section);

    /**
     * Export of the default section.
     *
     * @see #exportResult(String, Long, String, String)
     */
    default byte[] exportResult(String tenant, Long id, String format) {
        return exportResult(tenant, id, format, null);
    }

    /**
     * Log entries of the job (newest first), read with the tenant of the job.
     */
    List<LogEntry> getLogs(String tenant, Long id, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit);

    /**
     * Asks a running job (NORMALIZING / SOLVING) to stop: it becomes STOPPING.
     *
     * @throws eu.isygoit.exception.ConflictException (409) when the job is not running
     */
    void stop(String tenant, Long id);

    /**
     * Cancels a job that has not started (CREATED -&gt; CANCELLED).
     *
     * @throws eu.isygoit.exception.ConflictException (409) when the job already started or finished
     */
    void cancel(String tenant, Long id);

    /**
     * Dry-run validation (delegates to {@code PlanningService.validate}): reports ALL issues, the compatibility of the
     * request with every engine and the model estimate, without persisting anything. Never throws for invalid input:
     * failures are reported as issues.
     *
     * @param engineOrNull engine to judge validity against, {@code null} = the request engine
     */
    ValidationReportDto validate(PlanningRequest request, String engineOrNull);

    /**
     * Dry-run validation against the engine of the request.
     */
    default ValidationReportDto validate(PlanningRequest request) {
        return validate(request, null);
    }

    /**
     * What this instance can solve, per engine: solvers (availability, features), constraint and objective types with
     * per-engine support, schema version and effective limits.
     */
    CapabilitiesDto capabilities();

    /**
     * Counters and recent jobs of the tenant.
     *
     * @param limit number of recent jobs, between 1 and 500
     */
    JobDashboardSummary dashboard(String tenant, int limit);
}

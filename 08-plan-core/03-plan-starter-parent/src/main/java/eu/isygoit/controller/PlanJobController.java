package eu.isygoit.controller;

import eu.isygoit.annotation.InjectMapperAndService;
import eu.isygoit.api.PlanJobServiceApi;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.tenancy.MappedCrudTenantController;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.CapabilitiesDto;
import eu.isygoit.dto.response.JobDashboardSummary;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.OperationNotSupportedException;
import eu.isygoit.exception.handler.PlanExceptionHandler;
import eu.isygoit.mapper.PlanJobMapper;
import eu.isygoit.mapper.PlanJobMinMapper;
import eu.isygoit.model.PlanJob;
import eu.isygoit.service.IPlanJobService;
import eu.isygoit.service.impl.PlanJobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * CRUD + lifecycle controller for {@link PlanJob}.
 * <p>
 * Mirrors the pilot {@code StudentController}: stack mapped tenant CRUD (paged list, filter, get by id, count,
 * delete - the service only deletes terminal jobs) plus the planning endpoints of {@link PlanJobServiceApi}, all
 * delegating to {@link IPlanJobService}.
 * <ul>
 *     <li><b>Tenant</b>: always resolved from the authenticated caller
 *     ({@code requestContextService().getCurrentContext().getSenderTenant()}, as in the pilot) and passed to the
 *     service; a missing tenant is a 400 {@code TENANT_REQUIRED}. Planning request bodies do not carry tenant identity.</li>
 *     <li><b>Job creation</b> only goes through {@code POST /submit}. The inherited {@code POST ""},
 *     {@code POST /batch} and {@code PUT /{id}} endpoints are {@code final} in the stack base controller and cannot be
 *     overridden, so they are neutralised through the stack's {@code beforeCreate} / {@code beforeUpdate} hooks, which
 *     run before any mapping or service call and reject the call with an
 *     {@link OperationNotSupportedException} (HTTP 405, see {@link PlanExceptionHandler}). The request of a job is
 *     replaced through {@code PUT /{id}/request}.</li>
 *     <li><b>Errors</b>: every error becomes the {@link PlanExceptionHandler.ApiError} body (see
 *     {@link #getBackExceptionResponse(Throwable)}).</li>
 * </ul>
 */
@Slf4j
@Validated
@Tag(name = "PlanJob", description = "Endpoints for submitting and managing planning jobs")
@RestController
@RequestMapping(path = PlanJobController.BASE_PATH)
@InjectMapperAndService(handler = PlanExceptionHandler.class,
        mapper = PlanJobMapper.class,
        minMapper = PlanJobMinMapper.class,
        service = PlanJobService.class)
public class PlanJobController
        extends MappedCrudTenantController<Long, PlanJob, PlanJobMinDto, PlanJobDto, PlanJobService>
        implements PlanJobServiceApi {

    /**
     * Base path of the controller (pilot pattern {@code /api/v1/private/<entity>}).
     */
    public static final String BASE_PATH = "/api/v1/private/plan/job";

    /**
     * Default number of log entries returned by {@link #getLogs}.
     */
    static final int DEFAULT_LOG_LIMIT = 100;
    /**
     * Maximum number of log entries returned by {@link #getLogs}.
     */
    static final int MAX_LOG_LIMIT = 1000;
    /**
     * Maximum number of recent jobs of the dashboard.
     */
    static final int MAX_DASHBOARD_LIMIT = 500;

    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_CSV = "csv";
    private static final String DEFAULT_SECTION = "assignments";
    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv");

    // region Lifecycle endpoints

    @Override
    public ResponseEntity<PlanJobDto> submit(PlanningRequest request) {
        try {
            String tenant = requestContextService().getCurrentContext().getSenderTenant(); // an anonymous caller (no resolvable tenant) is rejected like on the other endpoints
            PlanJob job = planJobs().submit(tenant, request);
            return ResponseEntity.accepted().body(mapper().entityToDto(job));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<PlanningRequest> getRequest(Long id) {
        try {
            return ResponseEntity.ok(planJobs().getRequest(tenant(), id));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<PlanJobDto> updateRequest(Long id, PlanningRequest request) {
        try {
            PlanJob job = planJobs().updateRequest(tenant(), id, request);
            return ResponseEntity.ok(mapper().entityToDto(job));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<SolveResult> getResult(Long id) {
        try {
            return ResponseEntity.ok(planJobs().getResult(tenant(), id));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<byte[]> exportResult(Long id, String format, String section) {
        try {
            String tenant = tenant();
            String normalized = format == null || format.isBlank() ? FORMAT_JSON : format.trim().toLowerCase(Locale.ROOT);
            if (!FORMAT_JSON.equals(normalized) && !FORMAT_CSV.equals(normalized)) {
                throw new InvalidRequestException("UNSUPPORTED_EXPORT_FORMAT",
                        "Unsupported export format '" + format + "': use json or csv");
            }
            String normalizedSection = section == null || section.isBlank()
                    ? null : section.trim().toLowerCase(Locale.ROOT);
            PlanJob job = planJobs().getOwned(tenant, id);
            byte[] content = planJobs().exportResult(tenant, id, normalized, normalizedSection);
            // The file name carries the CSV section unless it is the default one (assignments).
            String suffix = FORMAT_CSV.equals(normalized) && normalizedSection != null
                    && !DEFAULT_SECTION.equals(normalizedSection) ? "-" + sanitize(normalizedSection) : "";
            String filename = "plan-job-" + sanitize(job.getCode()) + suffix + "." + normalized;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(filename).build().toString())
                    .contentType(FORMAT_CSV.equals(normalized) ? TEXT_CSV : MediaType.APPLICATION_JSON)
                    .body(content);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<List<LogEntry>> getLogs(Long id, Instant from, Instant to,
                                                  IEnumLogLevel.Types minLevel, int limit) {
        try {
            int effectiveLimit = clamp(limit, 1, MAX_LOG_LIMIT);
            return ResponseEntity.ok(planJobs().getLogs(tenant(), id, from, to, minLevel, effectiveLimit));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Void> stop(Long id) {
        try {
            planJobs().stop(tenant(), id);
            return ResponseEntity.accepted().build();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<Void> cancel(Long id) {
        try {
            planJobs().cancel(tenant(), id);
            return ResponseEntity.ok().build();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ValidationReportDto> validate(PlanningRequest request, String engine) {
        try {
            tenant(); // an anonymous caller (no resolvable tenant) is rejected like on the other endpoints
            return ResponseEntity.ok(planJobs().validate(request, engine));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<CapabilitiesDto> getCapabilities() {
        try {
            return ResponseEntity.ok(planJobs().capabilities());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<JobDashboardSummary> getDashboard(int limit) {
        try {
            int effectiveLimit = clamp(limit, 1, MAX_DASHBOARD_LIMIT);
            return ResponseEntity.ok(planJobs().dashboard(tenant(), effectiveLimit));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // endregion

    // region Neutralised inherited creation / update

    /**
     * Rejects the inherited {@code POST ""} and {@code POST /batch}: jobs are created by {@code POST /submit} only.
     *
     * @param dto ignored
     * @throws OperationNotSupportedException always (HTTP 405)
     */
    @Override
    public PlanJobDto beforeCreate(PlanJobDto dto) {
        throw new OperationNotSupportedException(
                "Planning jobs cannot be created directly: use POST " + BASE_PATH + "/submit");
    }

    /**
     * Rejects the inherited {@code PUT /{id}}: job fields are owned by the lifecycle; the planning request is replaced
     * through {@code PUT /{id}/request}.
     *
     * @param id  ignored
     * @param dto ignored
     * @throws OperationNotSupportedException always (HTTP 405)
     */
    @Override
    public PlanJobDto beforeUpdate(Long id, PlanJobDto dto) {
        throw new OperationNotSupportedException(
                "Planning jobs cannot be updated directly: use PUT " + BASE_PATH + "/{id}/request");
    }

    // endregion

    /**
     * Every error of the controller leaves as the plan {@link PlanExceptionHandler.ApiError} body (same shape as the
     * errors raised outside the controller methods and handled by the {@code @RestControllerAdvice} entry point of the
     * same handler), instead of the stack's plain message body.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public ResponseEntity getBackExceptionResponse(Throwable e) {
        return ((PlanExceptionHandler) exceptionHandler()).toResponse(e);
    }

    /**
     * The service of the controller, typed with the planning operations.
     */
    protected IPlanJobService planJobs() {
        return crudService();
    }

    /**
     * The tenant of the authenticated caller (pilot way).
     *
     * @throws InvalidRequestException {@code TENANT_REQUIRED} when it cannot be resolved
     */
    protected String tenant() {
        String tenant = requestContextService().getCurrentContext().getSenderTenant();
        if (tenant == null || tenant.isBlank()) {
            throw new InvalidRequestException("TENANT_REQUIRED", "The caller tenant could not be resolved");
        }
        return tenant;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Keeps the job code usable in a file name / header.
     */
    private static String sanitize(String code) {
        return code == null ? "job" : code.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

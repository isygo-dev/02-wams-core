package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.CapabilitiesDto;
import eu.isygoit.dto.response.JobDashboardSummary;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.enums.IEnumLogLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

/**
 * API of {@link eu.isygoit.model.PlanJob}: stack CRUD (paged list, filter by criteria, get by id, delete, count)
 * plus the planning specific endpoints below. The controller is mapped on {@code /api/v1/private/plan/job}
 * (pilot pattern {@code /api/v1/private/<entity>}); the paths here are relative to it.
 * <p>
 * Filter criteria ({@code GET /filter}, fields annotated {@code @Criteria} on the entity): {@code status},
 * {@code engine}, {@code clientRef}, {@code code}; submission range through {@code submittedAt >= x} and
 * {@code submittedAt <= y} (ISO-8601 instants).
 * Inherited creation ({@code POST ""}, {@code POST /batch}) is not the way to create a job: use {@code POST /submit}.
 */
public interface PlanJobServiceApi extends IMappedCrudApi<Long, PlanJobMinDto, PlanJobDto> {

    @Operation(summary = "Submit a planning request",
            description = "Validates the request, stores a new job (status CREATED) and queues it for solving. "
                    + "When the request carries a meta.clientRef already used by the tenant, the existing job is "
                    + "returned instead of creating a new one.")
    @ApiResponse(responseCode = "202", description = "Job accepted",
            content = @Content(schema = @Schema(implementation = PlanJobDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid planning request", content = @Content)
    @PostMapping(path = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PlanJobDto> submit(@RequestBody PlanningRequest request);

    @Operation(summary = "Get the planning request of a job",
            description = "The request as stored (normalized form is not returned).")
    @ApiResponse(responseCode = "200", description = "Request retrieved",
            content = @Content(schema = @Schema(implementation = PlanningRequest.class)))
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    @GetMapping(path = "/{id}/request", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PlanningRequest> getRequest(@PathVariable(name = "id") Long id);

    @Operation(summary = "Replace the planning request of a job",
            description = "Allowed only while the job is CREATED (not yet picked up by a worker).")
    @ApiResponse(responseCode = "200", description = "Request replaced",
            content = @Content(schema = @Schema(implementation = PlanJobDto.class)))
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Job is no longer CREATED", content = @Content)
    @PutMapping(path = "/{id}/request", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PlanJobDto> updateRequest(@PathVariable(name = "id") Long id,
                                             @RequestBody PlanningRequest request);

    @Operation(summary = "Get the solve result of a job",
            description = "The solution of a SUCCEEDED job.")
    @ApiResponse(responseCode = "200", description = "Result retrieved",
            content = @Content(schema = @Schema(implementation = SolveResult.class)))
    @ApiResponse(responseCode = "404", description = "Job not found or no result yet", content = @Content)
    @GetMapping(path = "/{id}/result", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SolveResult> getResult(@PathVariable(name = "id") Long id);

    @Operation(summary = "Export the solve result of a job",
            description = "Downloads the result as a file: format=json (default, the full result; the section is "
                    + "ignored) or format=csv (one section per call). CSV sections: assignments (default; one row "
                    + "per task and resource), resources (usage per resource), unassigned (unselected optional "
                    + "tasks), selected (selected optional tasks), violations (soft and relaxed instances), "
                    + "relaxations, core (infeasibility core items) and metrics (timings and tier values).")
    @ApiResponse(responseCode = "200", description = "File content",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
    @ApiResponse(responseCode = "400", description = "Unknown format or section", content = @Content)
    @ApiResponse(responseCode = "404", description = "Job not found or no result yet", content = @Content)
    @GetMapping(path = "/{id}/result/export")
    ResponseEntity<byte[]> exportResult(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "format", required = false, defaultValue = "json")
            @Parameter(description = "Export format: json or csv", example = "csv") String format,
            @RequestParam(name = "section", required = false)
            @Parameter(description = "CSV section: assignments (default), resources, unassigned, selected, "
                    + "violations, relaxations, core or metrics", example = "assignments") String section);

    /**
     * Export of the default section (assignments for csv, the full result for json).
     * Not a mapped endpoint: a plain default method kept so that callers written before the `section`
     * parameter keep compiling.
     */
    default ResponseEntity<byte[]> exportResult(Long id, String format) {
        return exportResult(id, format, null);
    }

    @Operation(summary = "Get the execution logs of a job",
            description = "Log entries of the job, newest first, optionally restricted to a time range and a "
                    + "minimum level.")
    @ApiResponse(responseCode = "200", description = "Logs retrieved",
            content = @Content(schema = @Schema(implementation = LogEntry.class)))
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    @GetMapping(path = "/{id}/logs", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<LogEntry>> getLogs(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "minLevel", required = false) IEnumLogLevel.Types minLevel,
            @RequestParam(name = "limit", required = false, defaultValue = "100") int limit);

    @Operation(summary = "Ask a running job to stop",
            description = "NORMALIZING / SOLVING -> STOPPING; the worker keeps the best solution found so far.")
    @ApiResponse(responseCode = "202", description = "Stop requested", content = @Content)
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Job is not running", content = @Content)
    @PostMapping(path = "/{id}/stop")
    ResponseEntity<Void> stop(@PathVariable(name = "id") Long id);

    @Operation(summary = "Cancel a job that has not started",
            description = "CREATED -> CANCELLED.")
    @ApiResponse(responseCode = "200", description = "Job cancelled", content = @Content)
    @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Job already started or finished", content = @Content)
    @PostMapping(path = "/{id}/cancel")
    ResponseEntity<Void> cancel(@PathVariable(name = "id") Long id);

    @Operation(summary = "Validate a planning request (dry run)",
            description = "Nothing is stored nor solved: returns whether the request is valid for the requested "
                    + "engine, its normalized form, all errors and warnings, the compatibility of the request with "
                    + "every engine and an estimate of the model size. The optional engine parameter overrides "
                    + "solverOptions.engine for the validity verdict.")
    @ApiResponse(responseCode = "200", description = "Validation report",
            content = @Content(schema = @Schema(implementation = ValidationReportDto.class)))
    @PostMapping(path = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ValidationReportDto> validate(
            @RequestBody PlanningRequest request,
            @RequestParam(name = "engine", required = false)
            @Parameter(description = "Engine to validate against (default: the request engine)",
                    example = "cp_sat") String engine);

    /**
     * Validation against the engine of the request. Not a mapped endpoint (see {@link #exportResult(Long, String)}).
     */
    default ResponseEntity<ValidationReportDto> validate(PlanningRequest request) {
        return validate(request, null);
    }

    @Operation(summary = "Get the capabilities of the plan service",
            description = "Per-engine matrix: solvers with availability and supported features, constraint types "
                    + "(hard / soft / relaxable per engine), objective types, schema version and effective limits.")
    @ApiResponse(responseCode = "200", description = "Capabilities",
            content = @Content(schema = @Schema(implementation = CapabilitiesDto.class)))
    @GetMapping(path = "/capabilities", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CapabilitiesDto> getCapabilities();

    @Operation(summary = "Get the job dashboard of the tenant",
            description = "Counters per status, queue depth, average duration, success rate and the most recent jobs.")
    @ApiResponse(responseCode = "200", description = "Dashboard summary",
            content = @Content(schema = @Schema(implementation = JobDashboardSummary.class)))
    @GetMapping(path = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<JobDashboardSummary> getDashboard(
            @RequestParam(name = "limit", required = false, defaultValue = "50")
            @Parameter(description = "Number of recent jobs", example = "50") int limit);
}

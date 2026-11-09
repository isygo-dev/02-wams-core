package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.CapabilitiesDto;
import eu.isygoit.dto.response.ConstraintViolation;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.JobDashboardSummary;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.Relaxation;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.dto.response.ValidationReportDto;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.ConflictException;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.JobNotFoundException;
import eu.isygoit.exception.JobQueueException;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.exception.OperationNotAllowedException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.PlanJob;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.PlanJobRepository;
import eu.isygoit.repository.PlanJobStatusCount;
import eu.isygoit.repository.PlanJobStatusView;
import eu.isygoit.service.IPlanJobService;
import eu.isygoit.service.IPlanLogService;
import eu.isygoit.service.impl.planning.PlanningRequestValidator;
import eu.isygoit.service.impl.planning.PlanningService;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.cpsat.CpSatSolverEngine;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service implementation of {@link PlanJob}: submission (validation, code generation, idempotency on the client
 * reference, dispatch), tenant-scoped reads, request replacement, result export (CSV sections), stop / cancel,
 * terminal-only delete, dry-run validation (delegated to {@code PlanningService.validate}), the per-engine
 * capability matrix and the dashboard.
 * <p>
 * The job code is generated like the pilot services do ({@code @InjectCodeGen} / {@code @InjectCodeGenKms}, see
 * {@code getNextCode()}). Job creation is NOT available through the inherited CRUD create: use {@link #submit}.
 * The worker-side state transitions (claim, solve, complete) live in {@link JobProcessor} and the recovery
 * classes; they work on {@link PlanJobRepository} directly.
 */
@Slf4j
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = PlanJobRepository.class)
public class PlanJobService
        extends CodeAssignableTenantService<Long, PlanJob, PlanJobRepository>
        implements IPlanJobService {

    /**
     * Audit actor stored in created_by / updated_by by the native statements.
     */
    static final String AUDIT_ACTOR = "plan-service";
    static final int MAX_DASHBOARD_JOBS = 500;
    static final int MAX_LOG_LIMIT = 1000;
    private static final String CODE_PREFIX = "PLN";
    static final String SECTION_ASSIGNMENTS = "assignments";
    /** CSV sections of {@link #exportResult}; the first one is the default. */
    static final List<String> CSV_SECTIONS = List.of(SECTION_ASSIGNMENTS, "resources", "unassigned", "selected",
            "violations", "relaxations", "core", "metrics");
    /** Display labels of the known engines (an unknown engine id is its own label). */
    private static final Map<String, String> SOLVER_LABELS = Map.of(
            SolverOptions.ENGINE_CP_SAT, "Google OR-Tools CP-SAT",
            SolverOptions.ENGINE_TIMEFOLD, "Timefold Solver");
    private static final Pattern PATH_PREFIX = Pattern.compile("^([A-Za-z]+(?:[.\\[][A-Za-z0-9_\\]]+)+)");

    private final PlanningService planningService;
    private final PlanningRequestValidator requestValidator;
    private final IPlanLogService logService;
    private final JobDispatchService dispatchService;
    private final ConstraintFamilyRegistry constraintFamilyRegistry;
    private final ObjectiveFamilyRegistry objectiveFamilyRegistry;
    private final SolverRegistry solverRegistry;
    private final PlanLimitsProperties limits;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * Creates the service.
     */
    public PlanJobService(
            PlanningService planningService,
            PlanningRequestValidator requestValidator,
            IPlanLogService logService,
            JobDispatchService dispatchService,
            ConstraintFamilyRegistry constraintFamilyRegistry,
            ObjectiveFamilyRegistry objectiveFamilyRegistry,
            SolverRegistry solverRegistry,
            PlanLimitsProperties limits,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.planningService = planningService;
        this.requestValidator = requestValidator;
        this.logService = logService;
        this.dispatchService = dispatchService;
        this.constraintFamilyRegistry = constraintFamilyRegistry;
        this.objectiveFamilyRegistry = objectiveFamilyRegistry;
        this.solverRegistry = solverRegistry;
        this.limits = limits;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ------------------------------------------------------------------------------------------ submission

    /**
     * Not transactional on purpose: the job must be committed before it is handed to the queue / executor, otherwise
     * a worker could look for a job it cannot see yet.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlanJob submit(String tenant, PlanningRequest submitted) {
        TenantScope.requireTenant(tenant);
        PlanningRequest request = submitted;
        // Full pipeline up to the index (structural + semantic + capability + limits): a request the engine could not
        // solve is rejected here, with the code of its first error, instead of failing later in the worker.
        planningService.compile(null, request, null);

        String clientRef = blankToNull(request.meta().clientRef());
        if (clientRef != null) {
            Optional<PlanJob> existing = repository().findByTenantAndClientRef(tenant, clientRef);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        String requestJson = writeJson(request);
        // Real serialized size (the validator's own check is only an estimate).
        requestValidator.validateRequestBytes(requestJson.getBytes(StandardCharsets.UTF_8).length);

        String code = getNextCode();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("The next job code could not be generated");
        }
        // Canonical id (aliases cp-sat / cpsat resolved): the column is filtered and displayed as stored.
        String engine = request.solverOptions() == null
                ? SolverOptions.DEFAULT_ENGINE
                : SolverRegistry.canonicalId(request.solverOptions().engine());
        Optional<Long> insertedId = repository().insertCreatedIfAbsent(
                tenant, code, clientRef, engine, Instant.now(), "Job created", requestJson, AUDIT_ACTOR);
        if (insertedId.isEmpty()) {
            // Lost the idempotency race on (tenant, clientRef): the winner's job is the answer.
            return repository().findByTenantAndClientRef(tenant, clientRef)
                    .orElseThrow(() -> new IllegalStateException(
                            "Job was not inserted and no existing job was found"));
        }
        PlanJob job = repository().findById(insertedId.get())
                .orElseThrow(() -> new IllegalStateException("The inserted job was not found: " + insertedId.get()));
        dispatch(job);
        return job;
    }

    private void dispatch(PlanJob job) {
        try {
            // Dispatch marker first: the recovery sweep must not republish a job being dispatched right now.
            transactionTemplate.execute(status ->
                    repository().markDispatched(job.getId(), Instant.now()));
            dispatchService.dispatch(job);
        } catch (RuntimeException e) {
            releaseDispatchClaimQuietly(job.getId());
            log.error("Could not dispatch created planning job {}; recovery will retry", job.getId(), e);
            throw new JobQueueException(e);
        }
    }

    private void releaseDispatchClaimQuietly(Long jobId) {
        try {
            transactionTemplate.execute(status -> repository().releaseDispatchClaim(jobId, false));
        } catch (RuntimeException exception) {
            log.error("Could not release the dispatch claim of planning job {}", jobId, exception);
        }
    }

    // ------------------------------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public PlanJob getOwned(String tenant, Long id) {
        TenantScope.requireTenant(tenant);
        return repository().findById(id)
                .filter(job -> tenant.equals(job.getTenant()))
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PlanningRequest getRequest(String tenant, Long id) {
        PlanJob job = getOwned(tenant, id);
        if (job.getRequestJson() == null) {
            throw new JobNotFoundException(id);
        }
        return readValue(job.getRequestJson(), PlanningRequest.class, "request");
    }

    @Override
    @Transactional(readOnly = true)
    public SolveResult getResult(String tenant, Long id) {
        PlanJob job = getOwned(tenant, id);
        if (job.getResultJson() != null) {
            return readValue(job.getResultJson(), SolveResult.class, "result");
        }
        if (job.getStatus() == IEnumJobStatusCode.Types.INFEASIBLE) {
            Diagnostics diagnostics = job.getDiagnosticsJson() == null
                    ? null
                    : readValue(job.getDiagnosticsJson(), Diagnostics.class, "diagnostics");
            return new SolveResult(IEnumSolverStatus.Types.INFEASIBLE, null, List.of(), diagnostics, null);
        }
        throw new JobNotFoundException(id);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportResult(String tenant, Long id, String format, String section) {
        String normalized = format == null || format.isBlank() ? "json" : format.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("json") && !normalized.equals("csv")) {
            throw new InvalidRequestException("INVALID_EXPORT_FORMAT",
                    "Unknown export format '" + format + "': expected json or csv");
        }
        String sectionName = section == null || section.isBlank()
                ? SECTION_ASSIGNMENTS : section.trim().toLowerCase(Locale.ROOT);
        if (!CSV_SECTIONS.contains(sectionName)) {
            throw new InvalidRequestException("INVALID_EXPORT_SECTION",
                    "Unknown export section '" + section + "': expected one of " + CSV_SECTIONS);
        }
        SolveResult result = getResult(tenant, id);
        if (normalized.equals("json")) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Could not serialize the planning result", e);
            }
        }
        return toCsv(result, sectionName).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * One CSV section of the result: a header plus one row per element. Absent parts of the result (no solution, no
     * diagnostics, no metrics) give a header-only file. Cells are RFC 4180 quoted when needed; instants are ISO-8601.
     */
    static String toCsv(SolveResult result, String section) {
        Csv csv = new Csv();
        PlanningSolution solution = result.solution();
        List<Assignment> assignments = solution == null ? List.of() : solution.assignments();
        switch (section) {
            case "resources" -> resourcesSection(csv, assignments);
            case "unassigned" -> {
                csv.row("taskId", "reasonCode", "forgonePrize", "reason");
                for (UnassignedTask task : solution == null ? List.<UnassignedTask>of() : solution.unassignedTasks()) {
                    csv.row(task.taskId(), task.reasonCode(), task.forgonePrize(), task.reason());
                }
            }
            case "selected" -> {
                csv.row("taskId", "startTick", "endTick", "start", "end");
                for (Assignment assignment : assignments) {
                    if (assignment.optional()) {
                        csv.row(assignment.taskId(), assignment.startTick(), assignment.endTick(),
                                assignment.start(), assignment.end());
                    }
                }
            }
            case "violations" -> {
                csv.row("kind", "constraintId", "taskId", "resourceId", "code", "impact", "message");
                if (solution != null) {
                    violationRows(csv, "SOFT", solution.softViolations());
                    violationRows(csv, "RELAXED", solution.hardViolations());
                }
            }
            case "relaxations" -> {
                csv.row("constraintId", "originalValue", "relaxedValue", "authorized", "violationCount", "measure",
                        "impactOnScore");
                for (Relaxation relaxation : result.relaxations()) {
                    csv.row(relaxation.constraintId(), relaxation.originalValue(), relaxation.relaxedValue(),
                            relaxation.authorized(), relaxation.violationCount(), relaxation.measure(),
                            relaxation.impactOnScore());
                }
            }
            case "core" -> {
                csv.row("kind", "id", "reasonCode", "message");
                Diagnostics diagnostics = result.diagnostics();
                if (diagnostics != null && diagnostics.core() != null) {
                    for (CoreItem item : diagnostics.core().items()) {
                        csv.row(item.kind(), item.id(), item.reasonCode(), item.message());
                    }
                }
            }
            case "metrics" -> metricsSection(csv, result);
            default -> assignmentsSection(csv, assignments);
        }
        return csv.toString();
    }

    /**
     * {@code taskId,optional,role,resourceId,amount,slotId,startTick,endTick,start,end}: one row per task and
     * resource (a task without resource gives one row with empty role, resource and amount).
     */
    private static void assignmentsSection(Csv csv, List<Assignment> assignments) {
        csv.row("taskId", "optional", "role", "resourceId", "amount", "slotId", "startTick", "endTick", "start", "end");
        for (Assignment assignment : assignments) {
            if (assignment.resources().isEmpty()) {
                csv.row(assignment.taskId(), assignment.optional(), null, null, null, assignment.slotId(),
                        assignment.startTick(), assignment.endTick(), assignment.start(), assignment.end());
                continue;
            }
            for (ResourceAssignment resource : assignment.resources()) {
                csv.row(assignment.taskId(), assignment.optional(), resource.role(), resource.resourceId(),
                        resource.amount(), assignment.slotId(), assignment.startTick(), assignment.endTick(),
                        assignment.start(), assignment.end());
            }
        }
    }

    /**
     * {@code resourceId,taskCount,loadTicks,firstStartTick,lastEndTick}: usage per used resource, ascending id.
     * {@code loadTicks} is the sum of the task durations (a task counts once per resource, whatever its amount).
     */
    private static void resourcesSection(Csv csv, List<Assignment> assignments) {
        csv.row("resourceId", "taskCount", "loadTicks", "firstStartTick", "lastEndTick");
        Map<String, long[]> usage = new TreeMap<>();
        for (Assignment assignment : assignments) {
            Set<String> seen = new HashSet<>();
            for (ResourceAssignment resource : assignment.resources()) {
                if (resource.resourceId() == null || !seen.add(resource.resourceId())) {
                    continue;
                }
                long[] row = usage.computeIfAbsent(resource.resourceId(),
                        key -> new long[]{0, 0, Long.MAX_VALUE, Long.MIN_VALUE});
                row[0]++;
                row[1] += Math.max(0, assignment.endTick() - assignment.startTick());
                row[2] = Math.min(row[2], assignment.startTick());
                row[3] = Math.max(row[3], assignment.endTick());
            }
        }
        usage.forEach((resourceId, row) -> csv.row(resourceId, row[0], row[1], row[2], row[3]));
    }

    private static void violationRows(Csv csv, String kind, List<ConstraintViolation> violations) {
        for (ConstraintViolation violation : violations) {
            csv.row(kind, violation.constraintId(), violation.taskId(), violation.resourceId(), violation.code(),
                    violation.impact(), violation.message());
        }
    }

    /**
     * {@code name,value}: status, the timing fields, then one {@code tier[k].value|bound|status} row per tier.
     */
    private static void metricsSection(Csv csv, SolveResult result) {
        csv.row("name", "value");
        csv.row("status", result.status());
        Metrics metrics = result.metrics();
        if (metrics == null) {
            return;
        }
        csv.row("durationMillis", metrics.durationMillis());
        csv.row("buildMillis", metrics.buildMillis());
        csv.row("solveMillis", metrics.solveMillis());
        csv.row("timeToFirstSolutionMillis", metrics.timeToFirstSolutionMillis());
        csv.row("workersUsed", metrics.workersUsed());
        for (TierValue tier : metrics.tiers()) {
            csv.row("tier[" + tier.tier() + "].value", tier.value());
            csv.row("tier[" + tier.tier() + "].bound", tier.bound());
            csv.row("tier[" + tier.tier() + "].status", tier.status());
        }
    }

    /**
     * Minimal RFC 4180 writer (LF line ends): null is an empty cell; a cell holding a comma, a quote or a line break
     * is quoted with doubled quotes.
     */
    private static final class Csv {
        private final StringBuilder out = new StringBuilder();

        void row(Object... cells) {
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(cell(cells[i]));
            }
            out.append('\n');
        }

        private static String cell(Object value) {
            if (value == null) {
                return "";
            }
            String text = value.toString();
            if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
                return "\"" + text.replace("\"", "\"\"") + "\"";
            }
            return text;
        }

        @Override
        public String toString() {
            return out.toString();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LogEntry> getLogs(
            String tenant, Long id, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit) {
        if (limit < 1 || limit > MAX_LOG_LIMIT) {
            throw new InvalidRequestException("INVALID_LOG_LIMIT", "limit must be between 1 and " + MAX_LOG_LIMIT);
        }
        PlanJob job = getOwned(tenant, id);
        if ((from != null || to != null) && (from == null || to == null || from.isAfter(to))) {
            throw new InvalidRequestException("INVALID_LOG_RANGE",
                    "from and to must both be provided and from must not be after to");
        }
        return logService.findLatest(job.getTenant(), id, from, to, minLevel, limit);
    }

    // ------------------------------------------------------------------------------------------ management

    @Override
    public PlanJob updateRequest(String tenant, Long id, PlanningRequest submitted) {
        PlanJob job = getOwned(tenant, id);
        if (job.getStatus() != IEnumJobStatusCode.Types.CREATED) {
            throw new ConflictException("The request can only be replaced while the job is CREATED (status: "
                    + job.getStatus() + ")");
        }
        PlanningRequest request = submitted;
        planningService.compile(id, request, null);
        String newRef = blankToNull(request.meta().clientRef());
        if (newRef != null && !newRef.equals(job.getClientRef())) {
            throw new InvalidRequestException("CLIENT_REF_MISMATCH",
                    "meta.clientRef cannot be changed: it is the idempotency reference of the job");
        }
        String requestJson = writeJson(request);
        requestValidator.validateRequestBytes(requestJson.getBytes(StandardCharsets.UTF_8).length);
        if (repository().updateCreatedRequest(id, requestJson) < 1) {
            throw new ConflictException("The job is no longer CREATED: its request was not replaced");
        }
        return getOwned(tenant, id);
    }

    @Override
    public void stop(String tenant, Long id) {
        PlanJob job = getOwned(tenant, id);
        if (repository().stopIfProcessing(id) < 1) {
            throw new ConflictException("Only a running job can be stopped (status: " + job.getStatus() + ")");
        }
    }

    @Override
    public void cancel(String tenant, Long id) {
        PlanJob job = getOwned(tenant, id);
        if (repository().cancelIfRunning(id) < 1) {
            throw new ConflictException("Only a job that has not started can be cancelled (status: "
                    + job.getStatus() + ")");
        }
    }

    /**
     * Deletes a FINISHED job (SUCCEEDED, INFEASIBLE, FAILED, CANCELLED) of the tenant; any other status is a 409.
     * The logs stay in Cassandra until their TTL expires.
     */
    @Override
    public void delete(String tenant, Long id) {
        PlanJob job = getOwned(tenant, id);
        if (repository().deleteIfTerminal(id) < 1) {
            throw new ConflictException("Only a finished job can be deleted (status: " + job.getStatus() + ")");
        }
    }

    /**
     * The stack batch delete hands over entities that carry only the id: every job is re-loaded by id and tenant
     * (a foreign or missing job is a not-found) and goes through the terminal-status rule of {@link #delete}.
     */
    @Override
    public void deleteBatch(String tenant, List<PlanJob> objects) {
        validateListNotEmpty(objects);
        objects.forEach(object -> delete(tenant, object.getId()));
    }

    /**
     * Jobs are created by {@link #submit} only.
     */
    @Override
    public PlanJob beforeCreate(String tenant, PlanJob object) {
        throw new OperationNotAllowedException("A job is created by the submit operation");
    }

    /**
     * The request is replaced by {@link #updateRequest}; the lifecycle fields are owned by the workers.
     */
    @Override
    public PlanJob beforeUpdate(String tenant, PlanJob object) {
        throw new OperationNotAllowedException("A job is changed by its lifecycle operations only");
    }

    // ------------------------------------------------------------------------------------------ validation

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ValidationReportDto validate(PlanningRequest request, String engineOrNull) {
        String engine = engineOrNull == null || engineOrNull.isBlank() ? null : engineOrNull.trim();
        try {
            ValidationReportDto report = planningService.validate(request, engine);
            return report != null ? report : invalid("INVALID_REQUEST", "The validation produced no report", null);
        } catch (LimitExceededException e) {
            return invalid(e.code(), e.getMessage(), e.limitName());
        } catch (InvalidRequestException e) {
            return invalid(e.code(), e.getMessage(), pathOf(e.getMessage()));
        } catch (RuntimeException e) {
            log.debug("Dry-run validation failed unexpectedly", e);
            return invalid("INVALID_REQUEST", e.getMessage() == null ? "The request is invalid" : e.getMessage(), null);
        }
    }

    private static ValidationReportDto invalid(String code, String message, String path) {
        List<ValidationReportDto.Issue> issues = new ArrayList<>();
        issues.add(ValidationReportDto.Issue.builder()
                .code(code).severity("ERROR").message(message).path(path).build());
        return ValidationReportDto.builder().valid(false).normalizedRequest(null).errors(issues).build();
    }

    private static String pathOf(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = PATH_PREFIX.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    // ------------------------------------------------------------------------------------------ capabilities

    /**
     * The per-engine capability matrix, built from the engines' {@link EngineCapabilities} and the family registries
     * (nothing is hard-coded): the descriptions are the i18n keys of the families (the UI bundles carry the texts),
     * the flat hard/soft/relaxable flags are the union over the engines. An engine whose capabilities cannot be read
     * is listed as unavailable with no support instead of failing the endpoint.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CapabilitiesDto capabilities() {
        Collection<String> constraintTypes = constraintFamilyRegistry.supportedTypes();
        Collection<String> objectiveTypes = objectiveFamilyRegistry.supportedTypes();

        Map<String, EngineCapabilities> perEngine = new TreeMap<>();
        List<CapabilitiesDto.SolverCapability> solvers = new ArrayList<>();
        for (String id : solverRegistry.supportedEngines()) {
            SolverEngine engine = solverRegistry.get(id);
            EngineCapabilities caps = capabilitiesOf(engine, id);
            boolean readable = caps != null;
            if (!readable) {
                caps = new EngineCapabilities(id, Set.of(), Map.of(), Set.of(), false);
            }
            perEngine.put(id, caps);
            List<String> supported = new ArrayList<>();
            List<String> unsupported = new ArrayList<>();
            for (EngineCapabilities.Feature feature : EngineCapabilities.Feature.values()) {
                (caps.supports(feature) ? supported : unsupported).add(feature.name());
            }
            solvers.add(CapabilitiesDto.SolverCapability.builder()
                    .id(id)
                    .label(SOLVER_LABELS.getOrDefault(id, id))
                    .available(readable && isAvailable(engine))
                    .provesOptimality(caps.provesOptimality())
                    .features(supported)
                    .unsupportedFeatures(unsupported)
                    .unsupportedConstraintTypes(new ArrayList<>(caps.unsupportedConstraintTypes(constraintTypes)))
                    .unsupportedObjectiveTypes(new ArrayList<>(caps.unsupportedObjectiveTypes(objectiveTypes)))
                    .build());
        }

        List<CapabilitiesDto.ConstraintCapability> constraints = new ArrayList<>();
        for (String type : constraintTypes) {
            ConstraintFamily family = constraintFamilyRegistry.get(type);
            Map<String, CapabilitiesDto.EngineSupport> engines = new TreeMap<>();
            boolean hard = false;
            boolean soft = false;
            boolean relaxable = false;
            for (Map.Entry<String, EngineCapabilities> entry : perEngine.entrySet()) {
                EnforcementSupport support = entry.getValue().constraints().getOrDefault(type, EnforcementSupport.NONE);
                engines.put(entry.getKey(), CapabilitiesDto.EngineSupport.builder()
                        .hardCapable(support.hard()).softCapable(support.soft()).relaxable(support.relaxable())
                        .build());
                hard |= support.hard();
                soft |= support.soft();
                relaxable |= support.relaxable();
            }
            constraints.add(CapabilitiesDto.ConstraintCapability.builder()
                    .type(type)
                    .description(family.descriptionKey())
                    .paramsHint(family.paramsHint() == null ? new ArrayList<>() : new ArrayList<>(family.paramsHint()))
                    .scopeKind(family.scopeKind() == null ? null : family.scopeKind().name())
                    .engines(engines)
                    .hardCapable(hard)
                    .softCapable(soft)
                    .relaxable(relaxable)
                    .build());
        }

        List<CapabilitiesDto.ObjectiveCapability> objectives = new ArrayList<>();
        for (String type : objectiveTypes) {
            ObjectiveFamily family = objectiveFamilyRegistry.get(type);
            List<String> engines = new ArrayList<>();
            for (Map.Entry<String, EngineCapabilities> entry : perEngine.entrySet()) {
                if (entry.getValue().objectives().contains(type)) {
                    engines.add(entry.getKey());
                }
            }
            objectives.add(CapabilitiesDto.ObjectiveCapability.builder()
                    .type(type)
                    .description(family.descriptionKey())
                    .params(family.paramsHint() == null ? new ArrayList<>() : new ArrayList<>(family.paramsHint()))
                    .reward(family.reward())
                    .engines(engines)
                    .build());
        }
        return CapabilitiesDto.builder()
                .solvers(solvers)
                .constraints(constraints)
                .objectives(objectives)
                .limits(limitsView())
                .build();
    }

    private EngineCapabilities capabilitiesOf(SolverEngine engine, String id) {
        try {
            return engine.capabilities();
        } catch (RuntimeException | LinkageError e) {
            log.warn("The capabilities of engine {} could not be read; it is reported as unavailable", id, e);
            return null;
        }
    }

    /**
     * The CP-SAT engine loads its native library lazily and may be unavailable on a node; any other engine is
     * available as soon as it is registered.
     */
    private static boolean isAvailable(SolverEngine engine) {
        return !(engine instanceof CpSatSolverEngine cpSat) || cpSat.isAvailable();
    }

    private CapabilitiesDto.LimitsView limitsView() {
        return CapabilitiesDto.LimitsView.builder()
                .maxTasks(limits.getMaxTasks())
                .maxResources(limits.getMaxResources())
                .maxSlots(limits.getMaxSlots())
                .maxConstraints(limits.getMaxConstraints())
                .maxHorizonTicks((int) Math.min(Integer.MAX_VALUE, limits.getMaxHorizonTicks()))
                .maxTimeSeconds(limits.getMaxTimeSeconds())
                .maxWorkers(limits.getMaxWorkers())
                .maxCandidates(limits.getMaxCandidates())
                .maxRequestBytes(limits.getMaxRequestBytes())
                .build();
    }

    // ------------------------------------------------------------------------------------------ dashboard

    @Override
    @Transactional(readOnly = true)
    public JobDashboardSummary dashboard(String tenant, int limit) {
        TenantScope.requireTenant(tenant);
        if (limit < 1 || limit > MAX_DASHBOARD_JOBS) {
            throw new InvalidRequestException("INVALID_JOB_LIMIT",
                    "limit must be between 1 and " + MAX_DASHBOARD_JOBS);
        }
        Map<IEnumJobStatusCode.Types, Long> counts = new EnumMap<>(IEnumJobStatusCode.Types.class);
        for (PlanJobStatusCount row : repository().countGroupedByStatusForTenant(tenant)) {
            counts.merge(row.getStatus(), row.getTotal(), Long::sum);
        }
        // One lightweight query (no payload columns): the recent list and the duration average share it.
        List<PlanJobStatusView> views = repository().findStatusViewsByTenantOrderBySubmittedAtDesc(
                tenant, PageRequest.of(0, MAX_DASHBOARD_JOBS));
        List<PlanJobMinDto> recent = views.stream().limit(limit).map(PlanJobService::toMinDto).toList();

        long succeeded = counts.getOrDefault(IEnumJobStatusCode.Types.SUCCEEDED, 0L);
        long infeasible = counts.getOrDefault(IEnumJobStatusCode.Types.INFEASIBLE, 0L);
        long failed = counts.getOrDefault(IEnumJobStatusCode.Types.FAILED, 0L);
        long created = counts.getOrDefault(IEnumJobStatusCode.Types.CREATED, 0L);
        long decided = succeeded + failed + infeasible;
        double successRate = decided == 0 ? 0.0 : (double) succeeded / decided;
        return new JobDashboardSummary(
                counts.values().stream().mapToLong(Long::longValue).sum(),
                created,
                counts.getOrDefault(IEnumJobStatusCode.Types.ACCEPTED, 0L),
                counts.getOrDefault(IEnumJobStatusCode.Types.VALIDATING, 0L),
                counts.getOrDefault(IEnumJobStatusCode.Types.NORMALIZING, 0L),
                counts.getOrDefault(IEnumJobStatusCode.Types.SOLVING, 0L),
                counts.getOrDefault(IEnumJobStatusCode.Types.STOPPING, 0L),
                succeeded,
                infeasible,
                failed,
                counts.getOrDefault(IEnumJobStatusCode.Types.CANCELLED, 0L),
                averageDurationMillis(views),
                successRate,
                created,
                recent);
    }

    /**
     * Mean (completedAt - startedAt) of the finished jobs among the given (most recent) ones; 0 when none.
     */
    private static long averageDurationMillis(List<PlanJobStatusView> views) {
        return Math.round(views.stream()
                .filter(view -> view.getStartedAt() != null && view.getCompletedAt() != null)
                .mapToLong(view -> Math.max(0L,
                        view.getCompletedAt().toEpochMilli() - view.getStartedAt().toEpochMilli()))
                .average()
                .orElse(0.0));
    }

    private static PlanJobMinDto toMinDto(PlanJobStatusView view) {
        return PlanJobMinDto.builder()
                .id(view.getId())
                .code(view.getCode())
                .tenant(view.getTenant())
                .clientRef(view.getClientRef())
                .engine(view.getEngine())
                .status(view.getStatus())
                .submittedAt(view.getSubmittedAt())
                .startedAt(view.getStartedAt())
                .completedAt(view.getCompletedAt())
                .errorCode(view.getErrorCode())
                .message(view.getMessage())
                .build();
    }

    // ------------------------------------------------------------------------------------------ helpers

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new InvalidRequestException(
                    "REQUEST_NOT_SERIALIZABLE", "The value could not be serialized", exception);
        }
    }

    private <V> V readValue(String json, Class<V> type, String what) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize the persisted planning " + what, exception);
        }
    }

    /**
     * Code format {@code PLN-000001}, one counter for the service (like the pilot services).
     */
    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.SUPER_TENANT_NAME)
                .entity(PlanJob.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix(CODE_PREFIX)
                .valueLength(6L)
                .codeValue(1L)
                .increment(1)
                .build();
    }
}

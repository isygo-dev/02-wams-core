package eu.isygoit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.CapabilitiesDto;
import eu.isygoit.dto.response.ConstraintViolation;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.InfeasibilityCore;
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
import eu.isygoit.model.PlanJob;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.repository.PlanJobRepository;
import eu.isygoit.repository.PlanJobStatusCount;
import eu.isygoit.repository.PlanJobStatusView;
import eu.isygoit.service.IPlanLogService;
import eu.isygoit.service.IRemoteNextCodeService;
import eu.isygoit.service.impl.planning.PlanningRequestValidator;
import eu.isygoit.service.impl.planning.PlanningService;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.EngineCapabilities.Feature;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.cpsat.CpSatSolverEngine;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.spi.ScopeKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanJobServiceTest {
    private static final String TENANT = "t1";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final PlanningService planningService = mock(PlanningService.class);
    private final PlanningRequestValidator requestValidator = mock(PlanningRequestValidator.class);
    private final IPlanLogService logService = mock(IPlanLogService.class);
    private final JobDispatchService dispatchService = mock(JobDispatchService.class);
    private final PlanJobRepository repository = mock(PlanJobRepository.class);
    private final IRemoteNextCodeService remoteNextCode = mock(IRemoteNextCodeService.class);
    private final SolverEngine timefold = mock(SolverEngine.class);
    private final CpSatSolverEngine cpSat = mock(CpSatSolverEngine.class);

    private PlanJobService service;

    @BeforeEach
    void setUp() {
        when(timefold.name()).thenReturn("timefold");
        when(cpSat.name()).thenReturn("cp_sat");
        when(cpSat.isAvailable()).thenReturn(false);

        service = newService(List.of(family("min_rest", ScopeKind.RESOURCES, "minRestTicks"),
                        family("no_overlap", ScopeKind.GROUPS, "groupByTagPrefix")),
                List.of(objective("minimize_makespan", false), objective("minimize_slot_cost", false),
                        objective("maximize_selected_value", true)));
    }

    private PlanJobService newService(List<ConstraintFamily> constraintFamilies,
                                      List<ObjectiveFamily> objectiveFamilies) {
        PlanJobService created = new PlanJobService(planningService, requestValidator, logService, dispatchService,
                new ConstraintFamilyRegistry(constraintFamilies), new ObjectiveFamilyRegistry(objectiveFamilies),
                new SolverRegistry(List.of(cpSat, timefold)), new PlanLimitsProperties(), objectMapper,
                mock(PlatformTransactionManager.class));
        ReflectionTestUtils.setField(created, "repository", repository);
        ReflectionTestUtils.setField(created, "remoteNextCodeService", remoteNextCode);
        when(remoteNextCode.generateNextCode(anyString(), anyString())).thenReturn(ResponseEntity.ok("PLN-000001"));
        return created;
    }

    private static ConstraintFamily family(String type, ScopeKind scope, String... hints) {
        ConstraintFamily family = mock(ConstraintFamily.class);
        when(family.type()).thenReturn(type);
        when(family.descriptionKey()).thenReturn("plan.constraint." + type + ".description");
        when(family.paramsHint()).thenReturn(List.of(hints));
        when(family.scopeKind()).thenReturn(scope);
        return family;
    }

    private static ObjectiveFamily objective(String type, boolean reward) {
        ObjectiveFamily family = mock(ObjectiveFamily.class);
        when(family.type()).thenReturn(type);
        when(family.descriptionKey()).thenReturn("plan.objective." + type + ".description");
        when(family.paramsHint()).thenReturn(List.of());
        when(family.reward()).thenReturn(reward);
        return family;
    }

    private PlanningRequest request(String tenant, String clientRef) {
        return request(tenant, clientRef, null);
    }

    private PlanningRequest request(String tenant, String clientRef, String engine) {
        SolverOptions options = engine == null ? null
                : new SolverOptions(engine, null, null, null, null, null, null, null, null, null, null, null, null);
        return new PlanningRequest(tenant, new PlanningMeta(clientRef, "2.0", "test", "en", "UTC"),
                null, null, null, null, null, null, null, null, null, options, null);
    }

    private PlanJob job(long id, String tenant, IEnumJobStatusCode.Types status) {
        return PlanJob.builder().id(id).tenant(tenant).code("PLN-" + id).status(status).build();
    }

    private void owns(PlanJob job) {
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));
    }

    // ------------------------------------------------------------------ submit

    @Test
    void submitGeneratesTheCodeStoresTheJobAndDispatchesIt() {
        PlanJob stored = job(5L, TENANT, IEnumJobStatusCode.Types.CREATED);
        when(repository.findByTenantAndClientRef(TENANT, "ref-1")).thenReturn(Optional.empty());
        when(repository.insertCreatedIfAbsent(eq(TENANT), eq("PLN-000001"), eq("ref-1"), eq("cp_sat"),
                any(Instant.class), anyString(), anyString(), eq("plan-service"))).thenReturn(Optional.of(5L));
        when(repository.findById(5L)).thenReturn(Optional.of(stored));

        PlanJob result = service.submit(TENANT, request(null, "ref-1"));

        assertSame(stored, result);
        verify(planningService).compile(isNull(), any(PlanningRequest.class), isNull());
        verify(remoteNextCode).generateNextCode("PlanJob", SchemaColumnConstantName.C_CODE);
        verify(repository).markDispatched(eq(5L), any(Instant.class));
        verify(dispatchService).dispatch(stored);
    }

    @Test
    void submitStoresTheCanonicalEngineIdWhateverTheAlias() {
        PlanJob stored = job(5L, TENANT, IEnumJobStatusCode.Types.CREATED);
        when(repository.findByTenantAndClientRef(TENANT, "ref-1")).thenReturn(Optional.empty());
        when(repository.insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(5L));
        when(repository.findById(5L)).thenReturn(Optional.of(stored));

        service.submit(TENANT, request(TENANT, "ref-1", "TimeFold"));
        verify(repository).insertCreatedIfAbsent(eq(TENANT), eq("PLN-000001"), eq("ref-1"), eq("timefold"),
                any(Instant.class), anyString(), anyString(), eq("plan-service"));

        service.submit(TENANT, request(TENANT, "ref-1", "cp-sat"));
        verify(repository).insertCreatedIfAbsent(eq(TENANT), eq("PLN-000001"), eq("ref-1"), eq("cp_sat"),
                any(Instant.class), anyString(), anyString(), eq("plan-service"));
    }

    @Test
    void submitRejectsWhatTheEngineCannotSolveBeforeStoringAnything() {
        when(planningService.compile(any(), any(PlanningRequest.class), any()))
                .thenThrow(new InvalidRequestException("ENGINE_UNSUPPORTED_FEATURE", "tasks[0]: FREE_START"));

        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.submit(TENANT, request(TENANT, "ref-1", "timefold")));

        assertEquals("ENGINE_UNSUPPORTED_FEATURE", error.code());
        verify(repository, never()).insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any());
        verify(dispatchService, never()).dispatch(any(PlanJob.class));
    }

    @Test
    void submitIsIdempotentOnTheClientReference() {
        PlanJob existing = job(5L, TENANT, IEnumJobStatusCode.Types.SOLVING);
        when(repository.findByTenantAndClientRef(TENANT, "ref-1")).thenReturn(Optional.of(existing));

        PlanJob result = service.submit(TENANT, request(TENANT, "ref-1"));

        assertSame(existing, result);
        verify(repository, never()).insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any());
        verify(remoteNextCode, never()).generateNextCode(any(), any());
        verify(dispatchService, never()).dispatch(any(PlanJob.class));
    }

    @Test
    void submitWithoutClientReferenceDoesNotLookForAnExistingJob() {
        PlanJob stored = job(6L, TENANT, IEnumJobStatusCode.Types.CREATED);
        when(repository.insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(6L));
        when(repository.findById(6L)).thenReturn(Optional.of(stored));

        service.submit(TENANT, request(TENANT, null));

        verify(repository, never()).findByTenantAndClientRef(any(), any());
        verify(dispatchService).dispatch(stored);
    }

    @Test
    void submitReturnsTheWinnerWhenTheIdempotencyRaceIsLost() {
        PlanJob winner = job(9L, TENANT, IEnumJobStatusCode.Types.CREATED);
        when(repository.findByTenantAndClientRef(TENANT, "ref-1"))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertSame(winner, service.submit(TENANT, request(TENANT, "ref-1")));
        verify(dispatchService, never()).dispatch(any(PlanJob.class));
    }

    @Test
    void submitRejectsARequestOfAnotherTenant() {
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> service.submit(TENANT, request("other", "ref-1")));

        assertEquals("TENANT_MISMATCH", error.code());
        verify(repository, never()).insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitLeavesTheJobCreatedAndReleasesTheClaimWhenDispatchFails() {
        PlanJob stored = job(5L, TENANT, IEnumJobStatusCode.Types.CREATED);
        when(repository.insertCreatedIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(5L));
        when(repository.findById(5L)).thenReturn(Optional.of(stored));
        doThrow(new IllegalStateException("broker unavailable")).when(dispatchService).dispatch(stored);

        assertThrows(JobQueueException.class, () -> service.submit(TENANT, request(TENANT, null)));

        verify(repository).releaseDispatchClaim(5L, false);
    }

    // ------------------------------------------------------------------ tenant isolation / reads

    @Test
    void aJobOfAnotherTenantBehavesLikeAMissingJob() {
        owns(job(5L, "other", IEnumJobStatusCode.Types.SUCCEEDED));

        assertThrows(JobNotFoundException.class, () -> service.getOwned(TENANT, 5L));
        assertThrows(JobNotFoundException.class, () -> service.getRequest(TENANT, 5L));
        assertThrows(JobNotFoundException.class, () -> service.getResult(TENANT, 5L));
        assertThrows(JobNotFoundException.class, () -> service.stop(TENANT, 5L));
        assertThrows(JobNotFoundException.class, () -> service.cancel(TENANT, 5L));
        assertThrows(JobNotFoundException.class, () -> service.delete(TENANT, 5L));
        assertThrows(JobNotFoundException.class,
                () -> service.getLogs(TENANT, 5L, null, null, null, 10));
        assertThrows(JobNotFoundException.class, () -> service.getOwned(TENANT, 404L));
        verify(repository, never()).deleteIfTerminal(any());
        verify(repository, never()).stopIfProcessing(any());
    }

    @Test
    void getRequestReturnsTheStoredRequestWhateverTheStatus() throws Exception {
        PlanJob finished = job(5L, TENANT, IEnumJobStatusCode.Types.SUCCEEDED);
        finished.setRequestJson(objectMapper.writeValueAsString(request(TENANT, "ref-1")));
        owns(finished);

        assertEquals("ref-1", service.getRequest(TENANT, 5L).meta().clientRef());
    }

    @Test
    void getResultRebuildsTheInfeasibleResultFromTheDiagnostics() throws Exception {
        PlanJob infeasible = job(5L, TENANT, IEnumJobStatusCode.Types.INFEASIBLE);
        Diagnostics diagnostics = new Diagnostics("No feasible assignment", List.of("task-1"),
                new InfeasibilityCore(false, "PRESOLVE_ANALYSIS",
                        List.of(new CoreItem("TASK", "task-1", "NO_ELIGIBLE_RESOURCE", "no resource"))),
                List.of(), Map.of());
        infeasible.setDiagnosticsJson(objectMapper.writeValueAsString(diagnostics));
        owns(infeasible);

        SolveResult result = service.getResult(TENANT, 5L);

        assertEquals(IEnumSolverStatus.Types.INFEASIBLE, result.status());
        assertNull(result.solution());
        assertEquals("No feasible assignment", result.diagnostics().message());
        assertEquals(List.of("task-1"), result.diagnostics().infeasibilityCore());
        assertEquals("NO_ELIGIBLE_RESOURCE", result.diagnostics().core().items().get(0).reasonCode());
    }

    @Test
    void getResultReturnsTheIncumbentOfAStoppedJob() throws Exception {
        PlanJob stopped = job(5L, TENANT, IEnumJobStatusCode.Types.CANCELLED);
        stopped.setResultJson(objectMapper.writeValueAsString(
                new SolveResult(IEnumSolverStatus.Types.STOPPED_FEASIBLE,
                        new PlanningSolution(List.of(), List.of(), List.of(), List.of(), List.of(), 0.5),
                        List.of(), null, null)));
        owns(stopped);

        SolveResult result = service.getResult(TENANT, 5L);

        assertEquals(IEnumSolverStatus.Types.STOPPED_FEASIBLE, result.status());
        assertNotNull(result.solution());
    }

    @Test
    void getResultOfAJobWithoutResultIsNotFound() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SOLVING));

        assertThrows(JobNotFoundException.class, () -> service.getResult(TENANT, 5L));
    }

    @Test
    void getLogsReadsWithTheTenantOfTheJob() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SUCCEEDED));
        List<LogEntry> logs = new java.util.ArrayList<>();
        when(logService.findLatest(TENANT, 5L, null, null, IEnumLogLevel.Types.WARN, 10)).thenReturn(logs);

        assertSame(logs, service.getLogs(TENANT, 5L, null, null, IEnumLogLevel.Types.WARN, 10));
    }

    @Test
    void getLogsValidatesLimitAndRange() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SUCCEEDED));
        Instant now = Instant.now();

        assertThrows(InvalidRequestException.class, () -> service.getLogs(TENANT, 5L, null, null, null, 0));
        assertThrows(InvalidRequestException.class, () -> service.getLogs(TENANT, 5L, null, null, null, 1001));
        assertThrows(InvalidRequestException.class, () -> service.getLogs(TENANT, 5L, now, null, null, 10));
        assertThrows(InvalidRequestException.class,
                () -> service.getLogs(TENANT, 5L, now, now.minusSeconds(1), null, 10));
    }

    // ------------------------------------------------------------------ export

    private static final Instant T8 = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant T9 = Instant.parse("2026-01-01T09:00:00Z");

    private PlanJob jobWithResult() throws Exception {
        Assignment assignment = new Assignment("task-1", false, 4, 8, T8, T9, "slot-1",
                List.of(new ResourceAssignment("res-1", "teacher", 1), new ResourceAssignment("res,2", "room", 2)));
        Assignment selected = new Assignment("opt-1", true, 8, 10, T9, Instant.parse("2026-01-01T09:30:00Z"),
                null, List.of(new ResourceAssignment("res-1", "teacher", 1)));
        Assignment alone = new Assignment("solo", false, 0, 1, T8, T8.plusSeconds(60), null, List.of());
        PlanningSolution solution = new PlanningSolution(
                List.of(assignment, selected, alone),
                List.of(new UnassignedTask("opt-2", "NOT_SELECTED", "not selected", "more time", 90L)),
                List.of(new ConstraintViolation("deadline-B", "task-9", null, "WINDOW", "late by 2", 2.0)),
                List.of(new ConstraintViolation("rest", "task-1", "res-1", "TOO_CLOSE", "gap, short", 10.0)),
                List.of(), 1.0);
        Metrics metrics = new Metrics(120, 20, 100, 15L, 4, null, null,
                List.of(new TierValue(0, 75, 70.0, "OPTIMAL"), new TierValue(1, 3, null, "FEASIBLE")));
        SolveResult result = new SolveResult(IEnumSolverStatus.Types.OPTIMAL, solution,
                List.of(new Relaxation("deadline-B", "RELAXABLE_WITH_AUTHORIZATION", "RELAXED", 2.0, true, 1,
                        List.of("task-9"), 2.0)),
                new Diagnostics("msg", List.of(), new InfeasibilityCore(true, "ASSUMPTIONS",
                        List.of(new CoreItem("CONSTRAINT", "no-overlap-ab", "CONSTRAINT", "pair, overlap"))),
                        List.of(), Map.of()),
                metrics);
        PlanJob job = job(5L, TENANT, IEnumJobStatusCode.Types.SUCCEEDED);
        job.setResultJson(objectMapper.writeValueAsString(result));
        owns(job);
        return job;
    }

    private String csv(String section) {
        return new String(service.exportResult(TENANT, 5L, "csv", section), StandardCharsets.UTF_8);
    }

    @Test
    void exportsTheResultAsPrettyJsonWhateverTheSection() throws Exception {
        jobWithResult();

        String json = new String(service.exportResult(TENANT, 5L, "json"), StandardCharsets.UTF_8);
        String withSection = new String(service.exportResult(TENANT, 5L, "json", "violations"), StandardCharsets.UTF_8);

        assertTrue(json.contains("\n"), "pretty printed");
        assertTrue(json.contains("\"task-1\""));
        assertEquals(json, withSection, "json is always the full result");
    }

    @Test
    void exportsOneAssignmentsRowPerTaskAndResourceByDefault() throws Exception {
        jobWithResult();

        String expected = "taskId,optional,role,resourceId,amount,slotId,startTick,endTick,start,end\n"
                + "task-1,false,teacher,res-1,1,slot-1,4,8,2026-01-01T08:00:00Z,2026-01-01T09:00:00Z\n"
                + "task-1,false,room,\"res,2\",2,slot-1,4,8,2026-01-01T08:00:00Z,2026-01-01T09:00:00Z\n"
                + "opt-1,true,teacher,res-1,1,,8,10,2026-01-01T09:00:00Z,2026-01-01T09:30:00Z\n"
                + "solo,false,,,,,0,1,2026-01-01T08:00:00Z,2026-01-01T08:01:00Z\n";
        assertEquals(expected, csv(null));
        assertEquals(expected, csv(" "));
        assertEquals(expected, csv("Assignments"));
    }

    @Test
    void exportsTheResourceUsageSection() throws Exception {
        jobWithResult();

        assertEquals("resourceId,taskCount,loadTicks,firstStartTick,lastEndTick\n"
                + "\"res,2\",1,4,4,8\n"
                + "res-1,2,6,4,10\n", csv("resources"));
    }

    @Test
    void exportsUnassignedAndSelectedSections() throws Exception {
        jobWithResult();

        assertEquals("taskId,reasonCode,forgonePrize,reason\nopt-2,NOT_SELECTED,90,not selected\n", csv("unassigned"));
        assertEquals("taskId,startTick,endTick,start,end\nopt-1,8,10,2026-01-01T09:00:00Z,2026-01-01T09:30:00Z\n",
                csv("selected"));
    }

    @Test
    void exportsViolationsRelaxationsCoreAndMetricsSections() throws Exception {
        jobWithResult();

        assertEquals("kind,constraintId,taskId,resourceId,code,impact,message\n"
                + "SOFT,rest,task-1,res-1,TOO_CLOSE,10.0,\"gap, short\"\n"
                + "RELAXED,deadline-B,task-9,,WINDOW,2.0,late by 2\n", csv("violations"));
        assertEquals("constraintId,originalValue,relaxedValue,authorized,violationCount,measure,impactOnScore\n"
                + "deadline-B,RELAXABLE_WITH_AUTHORIZATION,RELAXED,true,1,2.0,2.0\n", csv("relaxations"));
        assertEquals("kind,id,reasonCode,message\nCONSTRAINT,no-overlap-ab,CONSTRAINT,\"pair, overlap\"\n", csv("core"));
        assertEquals("name,value\nstatus,OPTIMAL\ndurationMillis,120\nbuildMillis,20\nsolveMillis,100\n"
                + "timeToFirstSolutionMillis,15\nworkersUsed,4\n"
                + "tier[0].value,75\ntier[0].bound,70.0\ntier[0].status,OPTIMAL\n"
                + "tier[1].value,3\ntier[1].bound,\ntier[1].status,FEASIBLE\n", csv("metrics"));
    }

    @Test
    void exportOfAResultWithoutSolutionGivesHeaderOnlySections() throws Exception {
        PlanJob job = job(5L, TENANT, IEnumJobStatusCode.Types.FAILED);
        job.setResultJson(objectMapper.writeValueAsString(
                new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(), null, null)));
        owns(job);

        for (String section : PlanJobService.CSV_SECTIONS) {
            String lines = csv(section);
            long rows = lines.lines().count();
            // metrics also carries the status row; every other section is just its header
            assertEquals(section.equals("metrics") ? 2 : 1, rows, section);
        }
    }

    @Test
    void exportRejectsAnUnknownFormatAndAnUnknownSection() throws Exception {
        jobWithResult();

        InvalidRequestException format = assertThrows(InvalidRequestException.class,
                () -> service.exportResult(TENANT, 5L, "xml"));
        InvalidRequestException section = assertThrows(InvalidRequestException.class,
                () -> service.exportResult(TENANT, 5L, "csv", "everything"));
        assertEquals("INVALID_EXPORT_FORMAT", format.code());
        assertEquals("INVALID_EXPORT_SECTION", section.code());
    }

    // ------------------------------------------------------------------ management

    @Test
    void updateRequestIsOnlyAllowedWhileCreated() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SOLVING));

        assertThrows(ConflictException.class, () -> service.updateRequest(TENANT, 5L, request(TENANT, null)));
        verify(repository, never()).updateCreatedRequest(any(), any());
    }

    @Test
    void updateRequestReplacesTheRequestOfACreatedJob() {
        PlanJob created = job(5L, TENANT, IEnumJobStatusCode.Types.CREATED);
        owns(created);
        when(repository.updateCreatedRequest(eq(5L), anyString())).thenReturn(1);

        assertSame(created, service.updateRequest(TENANT, 5L, request(TENANT, null)));
        verify(planningService).compile(eq(5L), any(PlanningRequest.class), isNull());
    }

    @Test
    void stopAndCancelAreConflictsWhenTheStatusDoesNotAllowThem() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SUCCEEDED));
        when(repository.stopIfProcessing(5L)).thenReturn(0);
        when(repository.cancelIfRunning(5L)).thenReturn(0);

        assertThrows(ConflictException.class, () -> service.stop(TENANT, 5L));
        assertThrows(ConflictException.class, () -> service.cancel(TENANT, 5L));
    }

    @Test
    void stopAndCancelSucceedWhenTheStatementChangedTheJob() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SOLVING));
        when(repository.stopIfProcessing(5L)).thenReturn(1);
        when(repository.cancelIfRunning(5L)).thenReturn(1);

        service.stop(TENANT, 5L);
        service.cancel(TENANT, 5L);

        verify(repository).stopIfProcessing(5L);
        verify(repository).cancelIfRunning(5L);
    }

    @Test
    void deleteOnlyRemovesTerminalJobs() {
        owns(job(5L, TENANT, IEnumJobStatusCode.Types.SOLVING));
        when(repository.deleteIfTerminal(5L)).thenReturn(0);

        assertThrows(ConflictException.class, () -> service.delete(TENANT, 5L));

        owns(job(6L, TENANT, IEnumJobStatusCode.Types.FAILED));
        when(repository.deleteIfTerminal(6L)).thenReturn(1);
        service.delete(TENANT, 6L);
        verify(repository).deleteIfTerminal(6L);
    }

    @Test
    void deleteBatchReloadsEachJobByIdAndTenant() {
        PlanJob foreign = job(7L, "other", IEnumJobStatusCode.Types.FAILED);
        owns(foreign);
        owns(job(6L, TENANT, IEnumJobStatusCode.Types.FAILED));
        when(repository.deleteIfTerminal(6L)).thenReturn(1);

        assertThrows(JobNotFoundException.class, () -> service.deleteBatch(TENANT,
                List.of(PlanJob.builder().id(6L).build(), PlanJob.builder().id(7L).build())));
        verify(repository, never()).deleteIfTerminal(7L);
    }

    @Test
    void creationAndGenericUpdateThroughTheCrudHooksAreRefused() {
        assertThrows(eu.isygoit.exception.OperationNotAllowedException.class,
                () -> service.beforeCreate(TENANT, new PlanJob()));
        assertThrows(eu.isygoit.exception.OperationNotAllowedException.class,
                () -> service.beforeUpdate(TENANT, new PlanJob()));
    }

    // ------------------------------------------------------------------ validate / capabilities

    @Test
    void validateDelegatesToThePlanningServiceWithTheRequestedEngine() {
        ValidationReportDto report = ValidationReportDto.builder().valid(true)
                .engineCompatible(Map.of("cp_sat", true, "timefold", false)).build();
        when(planningService.validate(any(PlanningRequest.class), any())).thenReturn(report);

        assertSame(report, service.validate(request(TENANT, null), "timefold"));
        assertSame(report, service.validate(request(TENANT, null), "  "));
        assertSame(report, service.validate(request(TENANT, null)));
        verify(planningService).validate(any(PlanningRequest.class), eq("timefold"));
        verify(planningService, org.mockito.Mockito.times(2)).validate(any(PlanningRequest.class), isNull());
    }

    @Test
    void validateNeverThrowsForInvalidInput() {
        when(planningService.validate(any(), any()))
                .thenThrow(new InvalidRequestException("INVALID_REQUEST", "meta.domainName is required"))
                .thenThrow(new IllegalArgumentException("boom"))
                .thenThrow(new NullPointerException())
                .thenReturn(null);

        ValidationReportDto first = service.validate(request(TENANT, null), null);
        ValidationReportDto second = service.validate(request(TENANT, null), null);
        ValidationReportDto third = service.validate(null, null);
        ValidationReportDto fourth = service.validate(request(TENANT, null), null);

        assertFalse(first.isValid());
        assertNull(first.getNormalizedRequest());
        assertEquals("INVALID_REQUEST", first.getErrors().get(0).getCode());
        assertEquals("ERROR", first.getErrors().get(0).getSeverity());
        assertEquals("meta.domainName", first.getErrors().get(0).getPath());
        assertFalse(second.isValid());
        assertFalse(third.isValid());
        assertEquals(1, third.getErrors().size());
        assertFalse(fourth.isValid());
    }

    private EngineCapabilities cpSatCapabilities() {
        return new EngineCapabilities("cp_sat", EnumSet.of(Feature.FREE_START, Feature.PRECEDENCES, Feature.RELAXATION),
                Map.of("min_rest", EnforcementSupport.ALL, "no_overlap", EnforcementSupport.HARD_ONLY),
                Set.of("minimize_makespan", "minimize_slot_cost", "maximize_selected_value"), true);
    }

    private EngineCapabilities timefoldCapabilities() {
        return new EngineCapabilities("timefold", EnumSet.of(Feature.SLOT_MODE, Feature.OPTIONAL_TASKS),
                Map.of(), Set.of("minimize_slot_cost"), false);
    }

    private void engines() {
        when(cpSat.capabilities()).thenReturn(cpSatCapabilities());
        when(timefold.capabilities()).thenReturn(timefoldCapabilities());
    }

    @Test
    void capabilitiesAreBuiltPerEngineFromTheEngineCapabilitiesAndTheFamilies() {
        engines();

        CapabilitiesDto capabilities = service.capabilities();

        assertEquals("2.0", capabilities.getSchemaVersion());
        assertEquals(2, capabilities.getSolvers().size());
        CapabilitiesDto.SolverCapability cp = capabilities.getSolvers().get(0);
        CapabilitiesDto.SolverCapability tf = capabilities.getSolvers().get(1);
        assertEquals("cp_sat", cp.getId());
        assertFalse(cp.isAvailable(), "CpSatSolverEngine.isAvailable() decides");
        assertTrue(cp.isProvesOptimality());
        assertEquals(List.of("FREE_START", "PRECEDENCES", "RELAXATION"), cp.getFeatures());
        assertFalse(cp.getUnsupportedFeatures().contains("FREE_START"));
        assertTrue(cp.getUnsupportedFeatures().contains("SETUPS"));
        assertEquals(Feature.values().length, cp.getFeatures().size() + cp.getUnsupportedFeatures().size());
        assertTrue(cp.getUnsupportedConstraintTypes().isEmpty());
        assertEquals("timefold", tf.getId());
        assertTrue(tf.isAvailable());
        assertFalse(tf.isProvesOptimality());
        assertEquals(List.of("min_rest", "no_overlap"), tf.getUnsupportedConstraintTypes());
        assertEquals(List.of("maximize_selected_value", "minimize_makespan"), tf.getUnsupportedObjectiveTypes());
        assertNotNull(cp.getLabel());
    }

    @Test
    void capabilitiesCarryTheFamilyMetadataAndTheUnionAndPerEngineSupport() {
        engines();

        CapabilitiesDto capabilities = service.capabilities();

        assertEquals(List.of("min_rest", "no_overlap"),
                capabilities.getConstraints().stream().map(CapabilitiesDto.ConstraintCapability::getType).toList());
        CapabilitiesDto.ConstraintCapability minRest = capabilities.getConstraints().get(0);
        assertEquals("plan.constraint.min_rest.description", minRest.getDescription(), "the i18n key, not a text");
        assertEquals(List.of("minRestTicks"), minRest.getParamsHint());
        assertEquals("RESOURCES", minRest.getScopeKind());
        assertTrue(minRest.getEngines().get("cp_sat").isHardCapable());
        assertTrue(minRest.getEngines().get("cp_sat").isSoftCapable());
        assertTrue(minRest.getEngines().get("cp_sat").isRelaxable());
        assertFalse(minRest.getEngines().get("timefold").isHardCapable());
        assertTrue(minRest.isHardCapable() && minRest.isSoftCapable() && minRest.isRelaxable());
        CapabilitiesDto.ConstraintCapability noOverlap = capabilities.getConstraints().get(1);
        assertTrue(noOverlap.isHardCapable());
        assertFalse(noOverlap.isSoftCapable());
        assertFalse(noOverlap.isRelaxable());

        CapabilitiesDto.ObjectiveCapability slotCost = capabilities.getObjectives().stream()
                .filter(o -> o.getType().equals("minimize_slot_cost")).findFirst().orElseThrow();
        assertEquals(List.of("cp_sat", "timefold"), slotCost.getEngines());
        assertEquals("plan.objective.minimize_slot_cost.description", slotCost.getDescription());
        CapabilitiesDto.ObjectiveCapability selected = capabilities.getObjectives().stream()
                .filter(o -> o.getType().equals("maximize_selected_value")).findFirst().orElseThrow();
        assertTrue(selected.isReward());
        assertEquals(List.of("cp_sat"), selected.getEngines());
    }

    @Test
    void capabilitiesExposeTheEffectiveLimits() {
        engines();

        CapabilitiesDto.LimitsView limits = service.capabilities().getLimits();

        PlanLimitsProperties defaults = new PlanLimitsProperties();
        assertEquals(defaults.getMaxTasks(), limits.getMaxTasks());
        assertEquals(defaults.getMaxTimeSeconds(), limits.getMaxTimeSeconds());
        assertEquals(defaults.getMaxWorkers(), limits.getMaxWorkers());
        assertEquals(defaults.getMaxRequestBytes(), limits.getMaxRequestBytes());
    }

    @Test
    void anEngineWhoseCapabilitiesCannotBeReadIsListedAsUnavailable() {
        when(cpSat.capabilities()).thenThrow(new IllegalStateException("registry broken"));
        when(cpSat.isAvailable()).thenReturn(true);
        when(timefold.capabilities()).thenReturn(timefoldCapabilities());

        CapabilitiesDto capabilities = service.capabilities();

        CapabilitiesDto.SolverCapability cp = capabilities.getSolvers().get(0);
        assertFalse(cp.isAvailable());
        assertTrue(cp.getFeatures().isEmpty());
        assertEquals(2, capabilities.getSolvers().size());
        assertFalse(capabilities.getConstraints().get(0).getEngines().get("cp_sat").isHardCapable());
    }

    // ------------------------------------------------------------------ dashboard

    private PlanJobStatusCount count(IEnumJobStatusCode.Types status, long total) {
        PlanJobStatusCount count = mock(PlanJobStatusCount.class);
        when(count.getStatus()).thenReturn(status);
        when(count.getTotal()).thenReturn(total);
        return count;
    }

    private PlanJobStatusView view(long id, IEnumJobStatusCode.Types status, Instant started, Instant completed) {
        PlanJobStatusView view = mock(PlanJobStatusView.class);
        when(view.getId()).thenReturn(id);
        when(view.getTenant()).thenReturn(TENANT);
        when(view.getStatus()).thenReturn(status);
        when(view.getStartedAt()).thenReturn(started);
        when(view.getCompletedAt()).thenReturn(completed);
        return view;
    }

    @Test
    void dashboardComputesCountsRatesQueueDepthAndAverageDuration() {
        when(repository.countGroupedByStatusForTenant(TENANT)).thenReturn(List.of(
                count(IEnumJobStatusCode.Types.CREATED, 3), count(IEnumJobStatusCode.Types.SUCCEEDED, 6),
                count(IEnumJobStatusCode.Types.FAILED, 1), count(IEnumJobStatusCode.Types.INFEASIBLE, 1),
                count(IEnumJobStatusCode.Types.CANCELLED, 4)));
        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        when(repository.findStatusViewsByTenantOrderBySubmittedAtDesc(eq(TENANT), any())).thenReturn(List.of(
                view(3, IEnumJobStatusCode.Types.CREATED, null, null),
                view(2, IEnumJobStatusCode.Types.SUCCEEDED, base, base.plusSeconds(10)),
                view(1, IEnumJobStatusCode.Types.FAILED, base, base.plusSeconds(20))));

        JobDashboardSummary summary = service.dashboard(TENANT, 2);

        assertEquals(15, summary.total());
        assertEquals(3, summary.queueDepth());
        assertEquals(3, summary.created());
        assertEquals(6, summary.succeeded());
        assertEquals(4, summary.cancelled());
        assertEquals(6.0 / 8.0, summary.successRate(), 1e-9);
        assertEquals(15_000L, summary.averageDurationMillis());
        assertEquals(2, summary.recentJobs().size());
        assertEquals(3L, summary.recentJobs().get(0).getId());
    }

    @Test
    void dashboardWithoutFinishedJobsHasZeroRateAndDuration() {
        when(repository.countGroupedByStatusForTenant(TENANT)).thenReturn(List.of());
        when(repository.findStatusViewsByTenantOrderBySubmittedAtDesc(eq(TENANT), any())).thenReturn(List.of());

        JobDashboardSummary summary = service.dashboard(TENANT, 10);

        assertEquals(0.0, summary.successRate());
        assertEquals(0L, summary.averageDurationMillis());
        assertEquals(0L, summary.queueDepth());
        assertTrue(summary.recentJobs().isEmpty());
    }

    @Test
    void dashboardRejectsAnInvalidLimit() {
        assertThrows(InvalidRequestException.class, () -> service.dashboard(TENANT, 0));
        assertThrows(InvalidRequestException.class, () -> service.dashboard(TENANT, 501));
        verify(repository, never()).countGroupedByStatusForTenant(any());
    }
}

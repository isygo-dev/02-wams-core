package eu.isygoit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.async.kafka.PlanLogKafkaProducer;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InfeasibleException;
import eu.isygoit.exception.SolverBusyException;
import eu.isygoit.repository.PlanJobRepository;
import eu.isygoit.repository.PlanJobStatusView;
import eu.isygoit.service.impl.planning.PlanningService;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverCancellationToken;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobProcessorReliabilityTest {
    private static final Long JOB_ID = 42L;

    private final PlanningService planningService = mock(PlanningService.class);
    private final PlanJobRepository jobRepository = mock(PlanJobRepository.class);
    private final PlanLogKafkaProducer logProducer = mock(PlanLogKafkaProducer.class);
    private final JobCancellationRegistry cancellationRegistry = new JobCancellationRegistry();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobProcessor jobProcessor = new JobProcessor(
            planningService, jobRepository, logProducer, objectMapper, cancellationRegistry,
            mock(PlatformTransactionManager.class));

    private void jobIsCreated() throws Exception {
        PlanJobStatusView view = mock(PlanJobStatusView.class);
        when(view.getId()).thenReturn(JOB_ID);
        when(view.getTenant()).thenReturn("test-tenant");
        when(view.getEngine()).thenReturn("cp_sat");
        when(view.getStatus()).thenReturn(IEnumJobStatusCode.Types.CREATED);
        when(jobRepository.findStatusViewById(JOB_ID)).thenReturn(Optional.of(view));
        when(jobRepository.findRequestJson(JOB_ID)).thenReturn(objectMapper.writeValueAsString(request()));
    }

    @Test
    void doesNotProcessJobWhenAnotherWorkerOwnsTheClaim() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(0);

        jobProcessor.process(JOB_ID);

        verify(planningService, never()).solve(any(Long.class), any(), any(SolverCancellationToken.class), any(SolveListener.class));
        verify(jobRepository, never()).markSolving(any(), any());
        verify(jobRepository, never()).markFailedIfSolving(any(), any(), any());
    }

    @Test
    void ignoresUnknownAndTerminalJobs() {
        jobProcessor.process(JOB_ID);

        PlanJobStatusView done = mock(PlanJobStatusView.class);
        when(done.getStatus()).thenReturn(IEnumJobStatusCode.Types.SUCCEEDED);
        when(jobRepository.findStatusViewById(7L)).thenReturn(Optional.of(done));
        jobProcessor.process(7L);

        verify(jobRepository, never()).claimForProcessing(any(), any());
    }

    @Test
    void completesStopRequestBeforeStartingTheSolver() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(1);
        when(jobRepository.markSolving(eq(JOB_ID), any())).thenReturn(0);
        when(jobRepository.completeStopped(JOB_ID)).thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(planningService, never()).solve(any(Long.class), any(), any(SolverCancellationToken.class), any(SolveListener.class));
        verify(jobRepository).completeStopped(JOB_ID);
    }

    @Test
    void marksUnexpectedSolverFailureAsTerminalJobFailure() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(1);
        when(jobRepository.markSolving(eq(JOB_ID), any())).thenReturn(1);
        doThrow(new IllegalStateException("solver crashed"))
                .when(planningService).solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class), any(SolveListener.class));
        when(jobRepository.markFailedIfSolving(eq(JOB_ID), eq("JOB_PROCESSING_FAILED"), any())).thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(jobRepository).markFailedIfSolving(JOB_ID, "JOB_PROCESSING_FAILED",
                "Planning job failed unexpectedly");
        verify(logProducer).publish(org.mockito.ArgumentMatchers.argThat(
                (LogEntry entry) -> JOB_ID.equals(entry.jobId()) && "test-tenant".equals(entry.tenant())));
    }

    @Test
    void solverBusyReleasesTheClaimInsteadOfFailingTheJob() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(1);
        when(jobRepository.markSolving(eq(JOB_ID), any())).thenReturn(1);
        doThrow(new SolverBusyException(2, 30))
                .when(planningService).solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class), any(SolveListener.class));
        when(jobRepository.releaseClaim(eq(JOB_ID), any())).thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(jobRepository).releaseClaim(eq(JOB_ID), any());
        verify(jobRepository, never()).markFailedIfSolving(any(), any(), any());
    }

    @Test
    void doesNotStartJobsWhileShuttingDown() {
        cancellationRegistry.beginShutdown();

        jobProcessor.process(JOB_ID);

        verify(jobRepository, never()).claimForProcessing(any(), any());
    }

    @Test
    void solveInterruptedByShutdownIsReleasedNotCompleted() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(1);
        when(jobRepository.markSolving(eq(JOB_ID), any())).thenReturn(1);
        when(jobRepository.releaseClaim(eq(JOB_ID), any())).thenReturn(1);
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class)))
                .thenAnswer(invocation -> {
                    cancellationRegistry.beginShutdown();
                    cancellationRegistry.cancelAll();
                    return new SolveResult(IEnumSolverStatus.Types.FEASIBLE, null, List.of(), null, null);
                });

        jobProcessor.process(JOB_ID);

        verify(jobRepository).releaseClaim(eq(JOB_ID), any());
        verify(jobRepository, never()).completeSolving(any(), any(), any(), any(), any(), any());
    }

    @Test
    void successfulSolvePersistsTheResultAndPublishesALog() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(1);
        when(jobRepository.markSolving(eq(JOB_ID), any())).thenReturn(1);
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class)))
                .thenReturn(new SolveResult(IEnumSolverStatus.Types.OPTIMAL, null, List.of(), null, null));
        when(jobRepository.completeSolving(eq(JOB_ID), eq("SUCCEEDED"), any(), any(), any(), eq("OPTIMAL")))
                .thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(jobRepository).completeSolving(eq(JOB_ID), eq("SUCCEEDED"), any(), any(Instant.class),
                org.mockito.ArgumentMatchers.isNull(), eq("OPTIMAL"));
        verify(logProducer).publish(any(LogEntry.class));
    }

    @Test
    void stoppedFeasibleResultIsPersistedAndTheJobEndsCancelled() throws Exception {
        jobIsSolving();
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class))).thenReturn(stoppedResult());
        when(jobRepository.completeStoppedWithResult(eq(JOB_ID), any(), any(Instant.class), eq("STOPPED_FEASIBLE")))
                .thenReturn(1);

        jobProcessor.process(JOB_ID);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(jobRepository).completeStoppedWithResult(eq(JOB_ID), json.capture(), any(Instant.class),
                eq("STOPPED_FEASIBLE"));
        assertTrue(json.getValue().contains("STOPPED_FEASIBLE"), "the incumbent result is what is stored");
        verify(jobRepository, never()).completeSolving(any(), any(), any(), any(), any(), any());
        verify(jobRepository, never()).completeStopped(any());
        verify(logProducer).publish(any(LogEntry.class));
    }

    @Test
    void stoppedFeasibleFallsBackToThePlainStopWhenTheStopWasAlreadyCompleted() throws Exception {
        jobIsSolving();
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class))).thenReturn(stoppedResult());
        when(jobRepository.completeStoppedWithResult(eq(JOB_ID), any(), any(Instant.class), any())).thenReturn(0);
        when(jobRepository.completeStopped(JOB_ID)).thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(jobRepository).completeStopped(JOB_ID);
        verify(jobRepository, never()).completeSolving(any(), any(), any(), any(), any(), any());
    }

    @Test
    void stoppedFeasibleInterruptedByShutdownIsReleasedNotPersisted() throws Exception {
        jobIsSolving();
        when(jobRepository.releaseClaim(eq(JOB_ID), any())).thenReturn(1);
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class))).thenAnswer(invocation -> {
            cancellationRegistry.beginShutdown();
            cancellationRegistry.cancelAll();
            return stoppedResult();
        });

        jobProcessor.process(JOB_ID);

        verify(jobRepository).releaseClaim(eq(JOB_ID), any());
        verify(jobRepository, never()).completeStoppedWithResult(any(), any(), any(), any());
    }

    @Test
    void unknownResultStillFailsTheJobWithSolverUnknown() throws Exception {
        jobIsSolving();
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class)))
                .thenReturn(new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(), null, null));
        when(jobRepository.completeSolving(eq(JOB_ID), eq("FAILED"), any(), any(), eq("SOLVER_UNKNOWN"), eq("UNKNOWN")))
                .thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(jobRepository).completeSolving(eq(JOB_ID), eq("FAILED"), any(), any(Instant.class),
                eq("SOLVER_UNKNOWN"), eq("UNKNOWN"));
        verify(jobRepository, never()).completeStoppedWithResult(any(), any(), any(), any());
    }

    @Test
    void infeasibleResultStoresTheDiagnosticsAndTheInfeasibleStatus() throws Exception {
        jobIsSolving();
        doThrow(new InfeasibleException(new Diagnostics("No feasible plan", List.of(), null, List.of(), null)))
                .when(planningService).solve(eq(JOB_ID), any(PlanningRequest.class),
                        any(SolverCancellationToken.class), any(SolveListener.class));
        when(jobRepository.markInfeasibleIfSolving(eq(JOB_ID), any(), any())).thenReturn(1);

        jobProcessor.process(JOB_ID);

        ArgumentCaptor<String> diagnostics = ArgumentCaptor.forClass(String.class);
        verify(jobRepository).markInfeasibleIfSolving(eq(JOB_ID), eq("No feasible plan"), diagnostics.capture());
        assertTrue(diagnostics.getValue().contains("No feasible plan"));
        verify(jobRepository, never()).completeSolving(any(), any(), any(), any(), any(), any());
    }

    @Test
    void theSolverReceivesAListenerThatPublishesThrottledProgressLogs() throws Exception {
        jobIsSolving();
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class))).thenAnswer(invocation -> {
            SolveListener listener = invocation.getArgument(3);
            listener.onProgress(new SolveProgress(100, 1, 12.0, 10.0, 0.1667, 0, "SEARCH"));
            // within the throttle interval: remembered but not published
            listener.onProgress(new SolveProgress(200, 2, 11.0, null, null, 0, "SEARCH"));
            return new SolveResult(IEnumSolverStatus.Types.OPTIMAL, null, List.of(), null, null);
        });
        when(jobRepository.completeSolving(eq(JOB_ID), eq("SUCCEEDED"), any(), any(), any(), eq("OPTIMAL")))
                .thenReturn(1);

        jobProcessor.process(JOB_ID);

        ArgumentCaptor<LogEntry> entries = ArgumentCaptor.forClass(LogEntry.class);
        verify(logProducer, times(2)).publish(entries.capture());
        LogEntry progress = entries.getAllValues().get(0);
        assertEquals("PROGRESS", progress.step());
        assertEquals(IEnumLogLevel.Types.INFO, progress.level());
        assertEquals(JOB_ID, progress.jobId());
        assertEquals("test-tenant", progress.tenant());
        assertEquals(100L, progress.durationMillis());
        assertEquals("phase=SEARCH tier=0 solutions=1 objective=12.0 bound=10.0 gap=16.67%", progress.message());
        assertEquals("SOLVE", entries.getAllValues().get(1).step());
    }

    @Test
    void aFailingLogPipelineNeverBreaksTheSolve() throws Exception {
        jobIsSolving();
        doThrow(new IllegalStateException("kafka down")).doNothing().when(logProducer).publish(any(LogEntry.class));
        when(planningService.solve(eq(JOB_ID), any(PlanningRequest.class), any(SolverCancellationToken.class),
                any(SolveListener.class))).thenAnswer(invocation -> {
            SolveListener listener = invocation.getArgument(3);
            listener.onProgress(new SolveProgress(100, 1, 12.0, 10.0, 0.1, 0, "SEARCH"));
            return new SolveResult(IEnumSolverStatus.Types.FEASIBLE, null, List.of(), null, null);
        });
        when(jobRepository.completeSolving(eq(JOB_ID), eq("SUCCEEDED"), any(), any(), any(), eq("FEASIBLE")))
                .thenReturn(1);

        jobProcessor.process(JOB_ID);

        verify(jobRepository).completeSolving(eq(JOB_ID), eq("SUCCEEDED"), any(), any(Instant.class), any(),
                eq("FEASIBLE"));
        verify(jobRepository, never()).markFailedIfSolving(any(), any(), any());
    }

    @Test
    void progressMessageIsLocaleIndependentAndMarksUnknownValues() {
        assertEquals("phase=BUILD tier=-2 solutions=0 objective=n/a bound=n/a gap=n/a",
                JobProcessor.describe(new SolveProgress(5, 0, null, null, null, -2, "BUILD")));
    }

    private PlanningRequest request() {
        return new PlanningRequest(
                new PlanningMeta("ref-1", "2.0", "test", "en", "UTC"),
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private void jobIsSolving() throws Exception {
        jobIsCreated();
        when(jobRepository.claimForProcessing(eq(JOB_ID), any(Instant.class))).thenReturn(1);
        when(jobRepository.markSolving(eq(JOB_ID), any())).thenReturn(1);
    }

    private static SolveResult stoppedResult() {
        return new SolveResult(IEnumSolverStatus.Types.STOPPED_FEASIBLE,
                new PlanningSolution(List.of(), List.of(), List.of(), List.of(), List.of(), 0.5),
                List.of(), null, null);
    }
}

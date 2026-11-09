package eu.isygoit.plan.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.plan.api.PlanningMeta;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.application.port.LogProducer;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.engine.SolverCancellationToken;
import eu.isygoit.plan.port.JobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobProcessorReliabilityTest {
    private final PlanningService planningService = mock(PlanningService.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobStatusCache statusCache = mock(JobStatusCache.class);
    private final LogProducer logProducer = mock(LogProducer.class);
    private final JobProcessor jobProcessor = new JobProcessor(
            planningService, jobRepository, statusCache, logProducer,
            new ObjectMapper(), new JobCancellationRegistry());

    @Test
    void doesNotProcessJobWhenAnotherWorkerOwnsTheClaim() {
        PlanningRequest request = request();
        UUID jobId = request.meta().jobId();
        when(jobRepository.findStatus(jobId)).thenReturn(Optional.of(createdStatus(jobId)));
        when(jobRepository.claimForProcessing(eq(jobId), any(Instant.class))).thenReturn(false);

        jobProcessor.process(request);

        verify(planningService, never()).solve(eq(request), any(SolverCancellationToken.class));
        verify(jobRepository, never()).markSolving(any(), any());
        verify(jobRepository, never()).markFailedIfSolving(any(), any(), any());
    }

    @Test
    void completesStopRequestBeforeStartingTheSolver() {
        PlanningRequest request = request();
        UUID jobId = request.meta().jobId();
        when(jobRepository.findStatus(jobId)).thenReturn(Optional.of(createdStatus(jobId)));
        when(jobRepository.claimForProcessing(eq(jobId), any(Instant.class))).thenReturn(true);
        when(jobRepository.markSolving(eq(jobId), any())).thenReturn(false);
        when(jobRepository.completeStopped(jobId)).thenReturn(true);

        jobProcessor.process(request);

        verify(planningService, never()).solve(eq(request), any(SolverCancellationToken.class));
        verify(jobRepository).completeStopped(jobId);
        verify(statusCache).put(org.mockito.ArgumentMatchers.argThat(
                (JobStatus status) -> status.status() == IEnumJobStatusCode.Types.CANCELLED));
    }

    @Test
    void marksUnexpectedSolverFailureAsTerminalJobFailure() {
        PlanningRequest request = request();
        UUID jobId = request.meta().jobId();
        when(jobRepository.findStatus(jobId)).thenReturn(Optional.of(createdStatus(jobId)));
        when(jobRepository.claimForProcessing(eq(jobId), any(Instant.class))).thenReturn(true);
        when(jobRepository.markSolving(eq(jobId), any())).thenReturn(true);
        doThrow(new IllegalStateException("solver crashed"))
                .when(planningService).solve(eq(request), any(SolverCancellationToken.class));
        when(jobRepository.markFailedIfSolving(
                eq(jobId), eq("JOB_PROCESSING_FAILED"), any())).thenReturn(true);

        jobProcessor.process(request);

        verify(jobRepository).markFailedIfSolving(jobId, "JOB_PROCESSING_FAILED",
                "Planning job failed unexpectedly");
        verify(statusCache).put(org.mockito.ArgumentMatchers.argThat(
                (JobStatus status) -> status.status() == IEnumJobStatusCode.Types.FAILED
                        && "JOB_PROCESSING_FAILED".equals(status.errorCode())));
    }

    private PlanningRequest request() {
        return new PlanningRequest(
                "test-tenant",
                new PlanningMeta(UUID.randomUUID(), "v1", "test", "en", "UTC"),
                null, List.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    private JobStatus createdStatus(UUID jobId) {
        return new JobStatus(jobId, IEnumJobStatusCode.Types.CREATED,
                Instant.now(), null, null, null, "Job created");
    }
}

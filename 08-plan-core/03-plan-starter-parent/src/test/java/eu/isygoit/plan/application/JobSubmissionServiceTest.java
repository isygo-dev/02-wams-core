package eu.isygoit.plan.application;

import eu.isygoit.plan.api.PlanningMeta;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobQueuePublisher;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.exception.JobQueueException;
import eu.isygoit.plan.port.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobSubmissionServiceTest {
    @Test
    void leavesPersistedJobCreatedWhenQueueDispatchFails() {
        PlanningService planningService = mock(PlanningService.class);
        JobRepository jobRepository = mock(JobRepository.class);
        JobStatusCache statusCache = mock(JobStatusCache.class);
        JobQueuePublisher queuePublisher = mock(JobQueuePublisher.class);
        PlanningRequest request = new PlanningRequest(
                "test-tenant",
                new PlanningMeta(UUID.randomUUID(), "v1", "test", "en", "UTC"),
                null, List.of(), List.of(), List.of(), List.of(), List.of(), null);
        when(jobRepository.findStatus(request.meta().jobId())).thenReturn(Optional.empty());
        when(jobRepository.saveCreated(any(JobStatus.class), eq(request))).thenReturn(true);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(queuePublisher).publish(request);

        JobSubmissionService submissionService = new JobSubmissionService(
                planningService, jobRepository, statusCache, queuePublisher,
                mock(JobProcessor.class), mock(AsyncTaskExecutor.class), true);

        assertThrows(JobQueueException.class, () -> submissionService.submit(request));

        verify(jobRepository).saveCreated(any(JobStatus.class), eq(request));
        verify(jobRepository, never()).markFailedIfSolving(any(), any(), any());
    }
}

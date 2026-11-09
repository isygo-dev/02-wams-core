package eu.isygoit.plan.application;

import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.plan.api.PlanningMeta;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.port.JobRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobQueryServiceManagementTest {
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobStatusCache statusCache = mock(JobStatusCache.class);
    private final PlanningService planningService = mock(PlanningService.class);
    private final JobQueryService queryService =
            new JobQueryService(jobRepository, statusCache, planningService);

    @Test
    void doesNotAllowEditingAfterProcessingStarts() {
        UUID jobId = UUID.randomUUID();
        PlanningRequest request = request(jobId);
        when(jobRepository.findStatus(jobId))
                .thenReturn(Optional.of(status(jobId, IEnumJobStatusCode.Types.SOLVING)));

        assertFalse(queryService.updateRequest(jobId, request));

        verify(planningService, never()).normalizeAndValidate(any());
        verify(jobRepository, never()).updateCreatedRequest(any(), any());
    }

    @Test
    void updatesARequestOnlyWhileCreated() {
        UUID jobId = UUID.randomUUID();
        PlanningRequest request = request(jobId);
        when(jobRepository.findStatus(jobId)).thenReturn(
                Optional.of(status(jobId, IEnumJobStatusCode.Types.CREATED)),
                Optional.of(status(jobId, IEnumJobStatusCode.Types.CREATED)));
        when(jobRepository.updateCreatedRequest(jobId, request)).thenReturn(true);

        assertTrue(queryService.updateRequest(jobId, request));

        verify(planningService).normalizeAndValidate(request);
        verify(jobRepository).updateCreatedRequest(jobId, request);
    }

    @Test
    void stopRequestTransitionsOnlyAnActiveJob() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findStatus(jobId)).thenReturn(
                Optional.of(status(jobId, IEnumJobStatusCode.Types.SOLVING)),
                Optional.of(status(jobId, IEnumJobStatusCode.Types.STOPPING)));
        when(jobRepository.stopIfProcessing(jobId)).thenReturn(true);

        assertTrue(queryService.stop(jobId));

        verify(statusCache).put(org.mockito.ArgumentMatchers.argThat(
                current -> current.status() == IEnumJobStatusCode.Types.STOPPING));
    }

    @Test
    void deletesOnlyTerminalJobs() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findStatus(jobId))
                .thenReturn(Optional.of(status(jobId, IEnumJobStatusCode.Types.SUCCEEDED)));
        when(jobRepository.deleteIfTerminal(jobId)).thenReturn(true);

        assertTrue(queryService.delete(jobId));

        verify(jobRepository).deleteIfTerminal(eq(jobId));
        verify(statusCache).evict(jobId);
    }

    private PlanningRequest request(UUID jobId) {
        return new PlanningRequest(
                "test-tenant",
                new PlanningMeta(jobId, "v1", "test", "en", "UTC"),
                null, List.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    private JobStatus status(UUID jobId, IEnumJobStatusCode.Types status) {
        return new JobStatus(jobId, status, Instant.now(), null, null, null, status.meaning());
    }
}

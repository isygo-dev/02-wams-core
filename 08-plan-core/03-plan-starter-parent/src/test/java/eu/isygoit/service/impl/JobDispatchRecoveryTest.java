package eu.isygoit.service.impl;

import eu.isygoit.repository.PlanJobRepository;
import eu.isygoit.repository.PlanJobStatusView;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobDispatchRecoveryTest {
    private final PlanJobRepository jobRepository = mock(PlanJobRepository.class);
    private final JobDispatchService dispatchService = mock(JobDispatchService.class);
    private final JobCancellationRegistry registry = new JobCancellationRegistry();

    private JobDispatchRecovery recovery() {
        return new JobDispatchRecovery(jobRepository, dispatchService, registry,
                mock(PlatformTransactionManager.class), 100, 60, 300, 5);
    }

    private void kafka(boolean enabled) {
        when(dispatchService.isKafkaEnabled()).thenReturn(enabled);
    }

    private void localQueueCapacity(int capacity) {
        kafka(false);
        when(dispatchService.localQueueRemainingCapacity()).thenReturn(capacity);
    }

    private void jobExists(long id, String tenant) {
        PlanJobStatusView view = mock(PlanJobStatusView.class);
        when(view.getId()).thenReturn(id);
        when(view.getTenant()).thenReturn(tenant);
        when(jobRepository.findStatusViewById(id)).thenReturn(Optional.of(view));
    }

    @Test
    void redispatchesOnlyJobsClaimedForDispatchWithTheRedispatchGrace() {
        kafka(true);
        jobExists(7L, "t1");
        when(jobRepository.claimForDispatch(any(Instant.class), any(Instant.class), eq(5), eq(100)))
                .thenReturn(List.of(7L));

        Instant before = Instant.now();
        recovery().recoverCreatedJobs();

        verify(dispatchService).dispatch(7L, "t1");
        org.mockito.ArgumentCaptor<Instant> now = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.ArgumentCaptor<Instant> stale = org.mockito.ArgumentCaptor.forClass(Instant.class);
        verify(jobRepository).claimForDispatch(now.capture(), stale.capture(), eq(5), eq(100));
        assertTrue(!now.getValue().isBefore(before));
        assertTrue(stale.getValue().equals(now.getValue().minusSeconds(300)));
    }

    @Test
    void doesNotDispatchWhenNothingIsClaimed() {
        kafka(true);
        when(jobRepository.claimForDispatch(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        recovery().recoverCreatedJobs();

        verify(dispatchService, never()).dispatch(any(Long.class), any());
    }

    @Test
    void aClaimedJobThatDisappearedIsReleasedWithoutDispatch() {
        kafka(true);
        when(jobRepository.claimForDispatch(any(), any(), anyInt(), anyInt())).thenReturn(List.of(9L));

        recovery().recoverCreatedJobs();

        verify(dispatchService, never()).dispatch(any(Long.class), any());
        verify(jobRepository).releaseDispatchClaim(9L, false);
    }

    @Test
    void dispatchFailureKeepsTheAttemptAndReleasesTheClaim() {
        kafka(true);
        jobExists(7L, "t1");
        when(jobRepository.claimForDispatch(any(), any(), anyInt(), anyInt())).thenReturn(List.of(7L));
        doThrow(new IllegalStateException("broker down")).when(dispatchService).dispatch(7L, "t1");

        recovery().recoverCreatedJobs();

        verify(jobRepository).releaseDispatchClaim(7L, false);
    }

    @Test
    void exhaustedDispatchAttemptsMoveTheJobToFailedWithDispatchFailed() {
        kafka(true);
        when(jobRepository.failExhaustedDispatch(
                any(Instant.class), eq(5), eq("DISPATCH_FAILED"), anyString(), eq(100)))
                .thenReturn(List.of(3L));

        recovery().recoverCreatedJobs();

        verify(jobRepository).failExhaustedDispatch(
                any(Instant.class), eq(5), eq("DISPATCH_FAILED"), anyString(), eq(100));
    }

    @Test
    void localModeFullQueueDefersWithoutClaiming() {
        localQueueCapacity(0);

        recovery().recoverCreatedJobs();

        verify(jobRepository, never()).claimForDispatch(any(), any(), anyInt(), anyInt());
    }

    @Test
    void localModeClaimsAtMostTheRemainingQueueCapacity() {
        localQueueCapacity(3);

        recovery().recoverCreatedJobs();

        verify(jobRepository).claimForDispatch(any(), any(), eq(5), eq(3));
    }

    @Test
    void rejectedExecutionReleasesTheClaimsAndRefundsTheAttempt() {
        localQueueCapacity(10);
        jobExists(1L, "t1");
        jobExists(2L, "t1");
        when(jobRepository.claimForDispatch(any(), any(), anyInt(), anyInt())).thenReturn(List.of(1L, 2L));
        doThrow(new TaskRejectedException("full")).when(dispatchService).dispatch(1L, "t1");

        recovery().recoverCreatedJobs();

        verify(jobRepository).releaseDispatchClaim(1L, true);
        verify(jobRepository).releaseDispatchClaim(2L, true);
        verify(jobRepository, never()).releaseDispatchClaim(any(), eq(false));
        verify(dispatchService, never()).dispatch(2L, "t1");
    }

    @Test
    void marksExpiredProcessingJobsFailed() {
        kafka(true);
        when(jobRepository.findExpiredProcessingJobIds(eq(60), any())).thenReturn(List.of(4L));
        when(jobRepository.failExpiredProcessing(
                4L, 60, "JOB_PROCESSING_TIMEOUT",
                "Processing exceeded the solver time limit plus the recovery grace period"))
                .thenReturn(1);

        recovery().recoverCreatedJobs();

        verify(jobRepository).failExpiredProcessing(
                4L, 60, "JOB_PROCESSING_TIMEOUT",
                "Processing exceeded the solver time limit plus the recovery grace period");
    }

    @Test
    void cancelsStoppingJobsWhoseWorkerNeverAcknowledgedTheStop() {
        kafka(true);
        when(jobRepository.findExpiredStoppingJobIds(eq(60), any())).thenReturn(List.of(5L));
        when(jobRepository.cancelExpiredStopping(5L, 60, JobDispatchRecovery.STALE_STOP_MESSAGE)).thenReturn(1);

        recovery().recoverCreatedJobs();

        verify(jobRepository).cancelExpiredStopping(5L, 60, JobDispatchRecovery.STALE_STOP_MESSAGE);
    }

    @Test
    void aFailingStepDoesNotStopTheOtherSteps() {
        kafka(true);
        when(jobRepository.findExpiredProcessingJobIds(anyInt(), any()))
                .thenThrow(new IllegalStateException("db"));

        recovery().recoverCreatedJobs();

        verify(jobRepository).claimForDispatch(any(), any(), eq(5), eq(100));
    }

    @Test
    void sweepsAreSkippedWhileShuttingDown() {
        registry.beginShutdown();

        recovery().recoverCreatedJobs();

        verify(jobRepository, never()).claimForDispatch(any(), any(), anyInt(), anyInt());
        verify(jobRepository, never()).findExpiredStoppingJobIds(anyInt(), any());
    }

    @Test
    void overlappingSweepIsSkipped() {
        kafka(true);
        JobDispatchRecovery recovery = recovery();
        when(jobRepository.findExpiredProcessingJobIds(anyInt(), any())).thenAnswer(invocation -> {
            recovery.recoverCreatedJobs(); // re-entrant call while the first sweep is running
            return List.of();
        });

        recovery.recoverCreatedJobs();

        verify(jobRepository).claimForDispatch(any(), any(), eq(5), eq(100));
        verify(jobRepository).findExpiredProcessingJobIds(anyInt(), any());
    }
}

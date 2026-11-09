package eu.isygoit.service.impl;

import eu.isygoit.repository.PlanJobRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JobWorkerShutdownTest {
    private final PlanJobRepository jobRepository = mock(PlanJobRepository.class);
    private final JobCancellationRegistry registry = new JobCancellationRegistry();

    @Test
    void cancelsRunningSolvesAndReleasesTheClaimsOfJobsStillRunningAfterTheWait() {
        Long jobId = 11L;
        JobCancellationRegistry.Registration registration = registry.register(jobId);
        JobWorkerShutdown shutdown = new JobWorkerShutdown(registry, jobRepository, mock(PlatformTransactionManager.class), 0);
        shutdown.start();

        shutdown.stop();

        assertTrue(registry.isShuttingDown());
        assertTrue(registration.token().isCancellationRequested());
        verify(jobRepository).releaseClaim(jobId, JobWorkerShutdown.RELEASE_MESSAGE);
    }

    @Test
    void jobsThatReturnedInTimeAreNotReleasedByTheShutdownBean() {
        Long jobId = 11L;
        JobCancellationRegistry.Registration registration = registry.register(jobId);
        // the worker returns as soon as it is cancelled and closes its registration
        registration.token().onCancellation(registration::close);
        JobWorkerShutdown shutdown = new JobWorkerShutdown(registry, jobRepository, mock(PlatformTransactionManager.class), 5);
        shutdown.start();

        shutdown.stop();

        verify(jobRepository, never()).releaseClaim(any(), any());
    }

    @Test
    void stopWithoutStartDoesNothing() {
        new JobWorkerShutdown(registry, jobRepository, mock(PlatformTransactionManager.class), 0).stop();

        assertTrue(!registry.isShuttingDown());
    }
}

package eu.isygoit.plan.application;

import eu.isygoit.plan.api.PlanningMeta;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobQueuePublisher;
import eu.isygoit.plan.port.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobDispatchRecoveryTest {
    @Test
    void republishesPersistedCreatedJobs() {
        JobRepository jobRepository = mock(JobRepository.class);
        JobQueuePublisher queuePublisher = mock(JobQueuePublisher.class);
        PlanningRequest request = new PlanningRequest(
                "test-tenant",
                new PlanningMeta(UUID.randomUUID(), "v1", "test", "en", "UTC"),
                null, List.of(), List.of(), List.of(), List.of(), List.of(), null);
        when(jobRepository.findCreatedRequests(100)).thenReturn(List.of(request));

        JobDispatchRecovery recovery = new JobDispatchRecovery(
                jobRepository, queuePublisher, mock(JobProcessor.class),
                mock(AsyncTaskExecutor.class), true, 100);
        recovery.recoverCreatedJobs();

        verify(queuePublisher).publish(request);
    }
}

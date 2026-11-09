package eu.isygoit.service.impl;

import eu.isygoit.async.kafka.PlanJobKafkaProducer;
import eu.isygoit.dto.tech.PlanJobMessage;
import eu.isygoit.model.PlanJob;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JobDispatchServiceTest {
    private final PlanJobKafkaProducer producer = mock(PlanJobKafkaProducer.class);
    private final JobProcessor processor = mock(JobProcessor.class);
    private final AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);

    private JobDispatchService service(boolean kafka) {
        return new JobDispatchService(producer, processor, executor, mock(ThreadPoolTaskExecutor.class), kafka);
    }

    @Test
    void kafkaModePublishesTheSmallJobMessage() {
        PlanJob job = PlanJob.builder().id(5L).tenant("t1").build();

        service(true).dispatch(job);

        verify(producer).dispatch(new PlanJobMessage(5L, "t1"));
        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    void localModeRunsTheProcessorOnTheExecutor() {
        service(false).dispatch(5L, "t1");

        org.mockito.ArgumentCaptor<Runnable> task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());
        task.getValue().run();
        verify(processor).process(5L);
        verify(producer, never()).dispatch(any());
    }
}

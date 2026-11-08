package eu.isygoit.plan.application;

import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobQueuePublisher;
import eu.isygoit.plan.port.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JobDispatchRecovery {
    private static final int MAX_BATCH_SIZE = 1000;

    private final JobRepository jobRepository;
    private final JobQueuePublisher queuePublisher;
    private final JobProcessor jobProcessor;
    private final AsyncTaskExecutor planJobExecutor;
    private final boolean kafkaEnabled;
    private final int batchSize;

    public JobDispatchRecovery(
            JobRepository jobRepository,
            JobQueuePublisher queuePublisher,
            JobProcessor jobProcessor,
            @Qualifier("planJobExecutor") AsyncTaskExecutor planJobExecutor,
            @Value("${app.plan.execution.kafka-enabled:true}") boolean kafkaEnabled,
            @Value("${app.plan.execution.recovery-batch-size:100}") int batchSize) {
        this.jobRepository = jobRepository;
        this.queuePublisher = queuePublisher;
        this.jobProcessor = jobProcessor;
        this.planJobExecutor = planJobExecutor;
        this.kafkaEnabled = kafkaEnabled;
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("recovery batch size must be between 1 and " + MAX_BATCH_SIZE);
        }
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.plan.execution.recovery-interval-ms:30000}")
    public void recoverCreatedJobs() {
        for (PlanningRequest request : jobRepository.findCreatedRequests(batchSize)) {
            try {
                if (kafkaEnabled) {
                    queuePublisher.publish(request);
                } else {
                    planJobExecutor.execute(() -> jobProcessor.process(request));
                }
            } catch (RuntimeException exception) {
                log.error("Could not recover dispatch for planning job {}",
                        request.meta().jobId(), exception);
            }
        }
    }
}

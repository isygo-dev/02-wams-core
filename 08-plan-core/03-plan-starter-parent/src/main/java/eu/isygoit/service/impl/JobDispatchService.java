package eu.isygoit.service.impl;

import eu.isygoit.async.kafka.PlanJobKafkaProducer;
import eu.isygoit.dto.tech.PlanJobMessage;
import eu.isygoit.model.PlanJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hands a persisted job to the workers: to Kafka (a small {@link PlanJobMessage}: job id and tenant; the worker loads
 * the request from the database) when {@code app.plan.execution.kafka-enabled} is true, otherwise to the bounded
 * local executor which runs {@link JobProcessor#process(Long)}.
 * <p>
 * Used by the submission and by the recovery sweep. Failures surface as {@link RuntimeException}
 * ({@link TaskRejectedException} when the local executor queue is full); the callers decide how to release the
 * dispatch claim.
 */
@Service
@Slf4j
public class JobDispatchService {
    private final PlanJobKafkaProducer kafkaProducer;
    private final JobProcessor jobProcessor;
    private final AsyncTaskExecutor planJobExecutor;
    private final ThreadPoolTaskExecutor planJobPool;
    private final boolean kafkaEnabled;

    /**
     * Creates the dispatcher.
     */
    public JobDispatchService(
            PlanJobKafkaProducer kafkaProducer,
            JobProcessor jobProcessor,
            @Qualifier("planJobExecutor") AsyncTaskExecutor planJobExecutor,
            @Qualifier("planJobExecutorPool") ThreadPoolTaskExecutor planJobPool,
            @Value("${app.plan.execution.kafka-enabled:true}") boolean kafkaEnabled) {
        this.kafkaProducer = kafkaProducer;
        this.jobProcessor = jobProcessor;
        this.planJobExecutor = planJobExecutor;
        this.planJobPool = planJobPool;
        this.kafkaEnabled = kafkaEnabled;
    }

    /**
     * Dispatches a job.
     *
     * @throws RuntimeException when the broker or the local executor refuses the job
     */
    public void dispatch(PlanJob job) {
        dispatch(job.getId(), job.getTenant());
    }

    /**
     * Dispatches a job by id (recovery sweep, which has no entity at hand).
     */
    public void dispatch(Long jobId, String tenant) {
        if (kafkaEnabled) {
            kafkaProducer.dispatch(new PlanJobMessage(jobId, tenant));
        } else {
            planJobExecutor.execute(() -> jobProcessor.process(jobId));
        }
    }

    /**
     * True when jobs go through Kafka (no local queue limit applies).
     */
    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    /**
     * Free places of the local executor queue; 0 when the pool is not usable. Meaningful for the local mode only.
     */
    public int localQueueRemainingCapacity() {
        try {
            ThreadPoolExecutor executor = planJobPool.getThreadPoolExecutor();
            return executor.getQueue().remainingCapacity();
        } catch (IllegalStateException exception) {
            return 0;
        }
    }
}

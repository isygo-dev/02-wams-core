package eu.isygoit.plan.application;

import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobQueuePublisher;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.exception.JobQueueException;
import eu.isygoit.plan.port.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class JobSubmissionService {
    private final PlanningService planningService;
    private final JobRepository jobRepository;
    private final JobStatusCache statusCache;
    private final JobQueuePublisher queuePublisher;
    private final JobProcessor jobProcessor;
    private final AsyncTaskExecutor planJobExecutor;
    private final boolean kafkaEnabled;

    public JobSubmissionService(
            PlanningService planningService,
            JobRepository jobRepository,
            JobStatusCache statusCache,
            JobQueuePublisher queuePublisher,
            JobProcessor jobProcessor,
            @Qualifier("planJobExecutor") AsyncTaskExecutor planJobExecutor,
            @Value("${app.plan.execution.kafka-enabled:true}") boolean kafkaEnabled) {
        this.planningService = planningService;
        this.jobRepository = jobRepository;
        this.statusCache = statusCache;
        this.queuePublisher = queuePublisher;
        this.jobProcessor = jobProcessor;
        this.planJobExecutor = planJobExecutor;
        this.kafkaEnabled = kafkaEnabled;
    }

    public JobStatus submit(PlanningRequest request) {
        planningService.normalizeAndValidate(request);
        var jobId = request.meta().jobId();
        var existing = jobRepository.findStatus(jobId);
        if (existing.isPresent()) {
            statusCache.put(existing.get());
            return existing.get();
        }

        JobStatus created = new JobStatus(jobId, IEnumJobStatusCode.Types.CREATED,
                Instant.now(), null, null, null, "Job created");
        if (!jobRepository.saveCreated(created, request)) {
            return jobRepository.findStatus(jobId)
                    .map(exist -> {
                        statusCache.put(exist);
                        return exist;
                    })
                    .orElseThrow(() -> new IllegalStateException(
                            "Job was not inserted and no existing status was found"));
        }
        statusCache.put(created);
        try {
            if (kafkaEnabled) {
                queuePublisher.publish(request);
            } else {
                planJobExecutor.execute(() -> jobProcessor.process(request));
            }
        } catch (RuntimeException e) {
            log.error("Could not dispatch created planning job {}; recovery will retry", jobId, e);
            throw new JobQueueException(e);
        }
        return created;
    }
}

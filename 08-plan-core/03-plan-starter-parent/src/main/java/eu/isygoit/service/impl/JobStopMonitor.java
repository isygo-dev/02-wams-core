package eu.isygoit.service.impl;

import eu.isygoit.repository.PlanJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JobStopMonitor {
    private static final int BATCH_SIZE = 100;

    private final PlanJobRepository jobRepository;
    private final JobCancellationRegistry cancellationRegistry;

    public JobStopMonitor(
            PlanJobRepository jobRepository,
            JobCancellationRegistry cancellationRegistry) {
        this.jobRepository = jobRepository;
        this.cancellationRegistry = cancellationRegistry;
    }

    @Scheduled(fixedDelayString = "${app.plan.execution.stop-poll-interval-ms:1000}")
    public void signalStopRequests() {
        // Only jobs solved by this instance own a token: with none running, skip the query entirely.
        // Otherwise a single indexed query (partial index on status = 'STOPPING').
        if (!cancellationRegistry.hasActive()) {
            return;
        }
        jobRepository.findStoppingJobIds(PageRequest.of(0, BATCH_SIZE))
                .forEach(cancellationRegistry::cancel);
    }
}

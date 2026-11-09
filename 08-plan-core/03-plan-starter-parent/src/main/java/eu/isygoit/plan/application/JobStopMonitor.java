package eu.isygoit.plan.application;

import eu.isygoit.plan.port.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JobStopMonitor {
    private static final int BATCH_SIZE = 100;

    private final JobRepository jobRepository;
    private final JobCancellationRegistry cancellationRegistry;

    public JobStopMonitor(
            JobRepository jobRepository,
            JobCancellationRegistry cancellationRegistry) {
        this.jobRepository = jobRepository;
        this.cancellationRegistry = cancellationRegistry;
    }

    @Scheduled(fixedDelayString = "${app.plan.execution.stop-poll-interval-ms:1000}")
    public void signalStopRequests() {
        jobRepository.findStoppingJobIds(BATCH_SIZE)
                .forEach(cancellationRegistry::cancel);
    }
}

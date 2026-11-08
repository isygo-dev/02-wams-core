package eu.isygoit.plan.application.port;

import eu.isygoit.plan.domain.JobStatus;

import java.util.Optional;
import java.util.UUID;

public interface JobStatusCache {
    Optional<JobStatus> get(UUID jobId);

    void put(JobStatus status);

    void evict(UUID jobId);
}

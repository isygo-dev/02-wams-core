package eu.isygoit.plan.port;

import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.SolveResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    boolean saveCreated(JobStatus status, PlanningRequest request);

    List<PlanningRequest> findCreatedRequests(int limit);

    void upsert(UUID jobId, SolveResult result);

    void updateStatus(JobStatus status);

    boolean claimForProcessing(UUID jobId, Instant startedAt);

    void markFailed(UUID jobId, String errorCode, String message);

    void markInfeasible(UUID jobId, String message, String diagnostics);

    Optional<JobStatus> findStatus(UUID jobId);

    List<JobStatus> findRecentStatuses(int limit);

    long countByStatus(IEnumJobStatusCode.Types status);

    long countAll();

    Optional<SolveResult> findResult(UUID jobId);

    boolean cancelIfRunning(UUID jobId);
}

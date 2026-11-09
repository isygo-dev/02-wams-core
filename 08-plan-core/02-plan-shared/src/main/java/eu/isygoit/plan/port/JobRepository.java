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

    Optional<PlanningRequest> findRequest(UUID jobId);

    boolean updateCreatedRequest(UUID jobId, PlanningRequest request);

    boolean claimForProcessing(UUID jobId, Instant startedAt);

    boolean markSolving(UUID jobId, JobStatus status);

    boolean completeSolving(UUID jobId, JobStatus status, SolveResult result);

    boolean markFailedIfSolving(UUID jobId, String errorCode, String message);

    boolean markInfeasibleIfSolving(UUID jobId, String message, String diagnostics);

    Optional<JobStatus> findStatus(UUID jobId);

    List<JobStatus> findRecentStatuses(int limit);

    long countByStatus(IEnumJobStatusCode.Types status);

    long countAll();

    Optional<SolveResult> findResult(UUID jobId);

    boolean cancelIfRunning(UUID jobId);

    boolean stopIfProcessing(UUID jobId);

    List<UUID> findStoppingJobIds(int limit);

    boolean completeStopped(UUID jobId);

    boolean deleteIfTerminal(UUID jobId);
}

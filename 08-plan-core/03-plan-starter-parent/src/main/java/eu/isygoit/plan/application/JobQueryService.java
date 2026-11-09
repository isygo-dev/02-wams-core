package eu.isygoit.plan.application;

import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.plan.api.JobDashboardSummary;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.exception.InvalidRequestException;
import eu.isygoit.plan.exception.JobNotFoundException;
import eu.isygoit.plan.port.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobQueryService {
    private final JobRepository jobRepository;
    private final JobStatusCache statusCache;
    private final PlanningService planningService;

    public JobQueryService(
            JobRepository jobRepository,
            JobStatusCache statusCache,
            PlanningService planningService) {
        this.jobRepository = jobRepository;
        this.statusCache = statusCache;
        this.planningService = planningService;
    }

    public JobStatus status(UUID jobId) {
        return statusCache.get(jobId)
                .or(() -> jobRepository.findStatus(jobId).map(status -> {
                    statusCache.put(status);
                    return status;
                }))
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public JobStatus refreshStatus(UUID jobId) {
        return jobRepository.findStatus(jobId)
                .map(status -> {
                    statusCache.put(status);
                    return status;
                })
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public SolveResult result(UUID jobId) {
        return jobRepository.findResult(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
    }

    public List<JobStatus> recentStatuses(int limit) {
        if (limit < 1 || limit > 500) {
            throw new InvalidRequestException("INVALID_JOB_LIMIT", "limit must be between 1 and 500");
        }
        return jobRepository.findRecentStatuses(limit);
    }

    public long countByStatus(IEnumJobStatusCode.Types status) {
        return jobRepository.countByStatus(status);
    }

    public long countAll() {
        return jobRepository.countAll();
    }

    public JobDashboardSummary dashboardSummary(int limit) {
        return new JobDashboardSummary(
                countAll(),
                countByStatus(IEnumJobStatusCode.Types.CREATED),
                countByStatus(IEnumJobStatusCode.Types.ACCEPTED),
                countByStatus(IEnumJobStatusCode.Types.VALIDATING),
                countByStatus(IEnumJobStatusCode.Types.NORMALIZING),
                countByStatus(IEnumJobStatusCode.Types.SOLVING),
                countByStatus(IEnumJobStatusCode.Types.STOPPING),
                countByStatus(IEnumJobStatusCode.Types.SUCCEEDED),
                countByStatus(IEnumJobStatusCode.Types.INFEASIBLE),
                countByStatus(IEnumJobStatusCode.Types.FAILED),
                countByStatus(IEnumJobStatusCode.Types.CANCELLED),
                recentStatuses(limit));
    }

    public boolean cancel(UUID jobId) {
        refreshStatus(jobId);
        boolean cancelled = jobRepository.cancelIfRunning(jobId);
        statusCache.evict(jobId);
        return cancelled;
    }

    public Optional<PlanningRequest> request(UUID jobId) {
        JobStatus current = refreshStatus(jobId);
        if (current.status() != IEnumJobStatusCode.Types.CREATED) {
            return Optional.empty();
        }
        return jobRepository.findRequest(jobId);
    }

    public boolean updateRequest(UUID jobId, PlanningRequest request) {
        JobStatus current = refreshStatus(jobId);
        if (current.status() != IEnumJobStatusCode.Types.CREATED) {
            return false;
        }
        if (request == null || request.meta() == null || !jobId.equals(request.meta().jobId())) {
            throw new InvalidRequestException(
                    "JOB_ID_MISMATCH", "The request job ID must match the target job");
        }
        planningService.normalizeAndValidate(request);
        if (!jobRepository.updateCreatedRequest(jobId, request)) {
            statusCache.evict(jobId);
            return false;
        }
        statusCache.evict(jobId);
        refreshStatus(jobId);
        return true;
    }

    public boolean stop(UUID jobId) {
        refreshStatus(jobId);
        boolean stopping = jobRepository.stopIfProcessing(jobId);
        statusCache.evict(jobId);
        if (stopping) {
            refreshStatus(jobId);
        }
        return stopping;
    }

    public boolean delete(UUID jobId) {
        refreshStatus(jobId);
        boolean deleted = jobRepository.deleteIfTerminal(jobId);
        if (deleted) {
            statusCache.evict(jobId);
        }
        return deleted;
    }
}

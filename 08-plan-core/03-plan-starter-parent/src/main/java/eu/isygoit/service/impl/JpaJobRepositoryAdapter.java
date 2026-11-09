package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.Diagnostics;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.port.JobRepository;
import eu.isygoit.repository.PlanJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaJobRepositoryAdapter implements JobRepository {
    private final PlanJobRepository repository;
    private final ObjectMapper objectMapper;

    public JpaJobRepositoryAdapter(PlanJobRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean saveCreated(JobStatus status, PlanningRequest request) {
        return repository.insertCreatedIfAbsent(
                status.jobId(), status.submittedAt(), status.message(), writeJson(request)) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanningRequest> findCreatedRequests(int limit) {
        return repository.findCreatedRequestJson(PageRequest.of(0, limit)).stream()
                .map(this::readRequest)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanningRequest> findRequest(UUID jobId) {
        return Optional.ofNullable(repository.findRequestJson(jobId)).map(this::readRequest);
    }

    @Override
    public boolean updateCreatedRequest(UUID jobId, PlanningRequest request) {
        return repository.updateCreatedRequest(jobId, writeJson(request)) > 0;
    }

    @Override
    public boolean claimForProcessing(UUID jobId, Instant startedAt) {
        return repository.claimForProcessing(jobId, startedAt) > 0;
    }

    @Override
    public boolean markSolving(UUID jobId, JobStatus status) {
        return repository.markSolving(jobId, status.message()) > 0;
    }

    @Override
    public boolean completeSolving(UUID jobId, JobStatus status, SolveResult result) {
        return repository.completeSolving(
                jobId,
                status.status().name(),
                writeJson(result),
                status.finishedAt(),
                status.errorCode(),
                status.message()) > 0;
    }

    @Override
    public boolean markFailedIfSolving(UUID jobId, String errorCode, String message) {
        return repository.markFailedIfSolving(jobId, errorCode, message) > 0;
    }

    @Override
    public boolean markInfeasibleIfSolving(UUID jobId, String message, String diagnostics) {
        return repository.markInfeasibleIfSolving(jobId, message, diagnostics) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobStatus> findStatus(UUID jobId) {
        return repository.findById(jobId).map(entity -> new JobStatus(
                entity.getId(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorCode(),
                entity.getMessage()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobStatus> findRecentStatuses(int limit) {
        return repository.findAllByOrderBySubmittedAtDesc(PageRequest.of(0, limit)).stream()
                .map(entity -> new JobStatus(
                        entity.getId(),
                        entity.getStatus(),
                        entity.getSubmittedAt(),
                        entity.getStartedAt(),
                        entity.getCompletedAt(),
                        entity.getErrorCode(),
                        entity.getMessage()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(IEnumJobStatusCode.Types status) {
        return repository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolveResult> findResult(UUID jobId) {
        return repository.findById(jobId).flatMap(entity -> {
            if (entity.getResultJson() != null) {
                SolveResult result = readResult(entity.getResultJson());
                if (result != null) {
                    return Optional.of(result);
                }
            }
            if (entity.getStatus() == IEnumJobStatusCode.Types.INFEASIBLE) {
                Diagnostics diagnostics = entity.getDiagnosticsJson() == null
                        ? null
                        : readDiagnostics(entity.getDiagnosticsJson());
                return Optional.of(new SolveResult(
                        IEnumSolverStatus.Types.INFEASIBLE, null, List.of(), diagnostics, null));
            }
            return Optional.empty();
        });
    }

    @Override
    public boolean cancelIfRunning(UUID jobId) {
        return repository.cancelIfRunning(jobId) > 0;
    }

    @Override
    public boolean stopIfProcessing(UUID jobId) {
        return repository.stopIfProcessing(jobId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findStoppingJobIds(int limit) {
        return repository.findStoppingJobIds(PageRequest.of(0, limit));
    }

    @Override
    public boolean completeStopped(UUID jobId) {
        return repository.completeStopped(jobId) > 0;
    }

    @Override
    public boolean deleteIfTerminal(UUID jobId) {
        return repository.deleteIfTerminal(jobId) > 0;
    }

    private String writeJson(SolveResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize planning result", exception);
        }
    }

    private String writeJson(PlanningRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize planning request", exception);
        }
    }

    private PlanningRequest readRequest(String json) {
        try {
            return objectMapper.readValue(json, PlanningRequest.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize persisted planning request", exception);
        }
    }

    private SolveResult readResult(String json) {
        try {
            return objectMapper.readValue(json, SolveResult.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize persisted planning result", exception);
        }
    }

    private Diagnostics readDiagnostics(String json) {
        try {
            return objectMapper.readValue(json, Diagnostics.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize persisted planning diagnostics", exception);
        }
    }
}

package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.model.PlanJob;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.Diagnostics;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.exception.JobNotFoundException;
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
    public void upsert(UUID jobId, SolveResult result) {
        PlanJob entity = getEntity(jobId);
        entity.setResultJson(writeJson(result));
        repository.save(entity);
    }

    @Override
    public void updateStatus(JobStatus status) {
        PlanJob entity = getEntity(status.jobId());
        entity.setStatus(status.status());
        entity.setStartedAt(status.startedAt());
        entity.setCompletedAt(status.finishedAt());
        entity.setErrorCode(status.errorCode());
        entity.setMessage(status.message());
        repository.save(entity);
    }

    @Override
    public boolean claimForProcessing(UUID jobId, Instant startedAt) {
        return repository.claimForProcessing(jobId, startedAt) > 0;
    }

    @Override
    public void markFailed(UUID jobId, String errorCode, String message) {
        PlanJob entity = getEntity(jobId);
        entity.setStatus(IEnumJobStatusCode.Types.FAILED);
        entity.setCompletedAt(Instant.now());
        entity.setErrorCode(errorCode);
        entity.setMessage(message);
        repository.save(entity);
    }

    @Override
    public void markInfeasible(UUID jobId, String message, String diagnostics) {
        PlanJob entity = getEntity(jobId);
        entity.setStatus(IEnumJobStatusCode.Types.INFEASIBLE);
        entity.setCompletedAt(Instant.now());
        entity.setErrorCode("INFEASIBLE");
        entity.setMessage(message);
        entity.setDiagnosticsJson(diagnostics);
        repository.save(entity);
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

    private PlanJob getEntity(UUID jobId) {
        return repository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
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

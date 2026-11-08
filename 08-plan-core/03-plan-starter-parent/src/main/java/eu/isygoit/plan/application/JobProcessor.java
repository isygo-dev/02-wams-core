package eu.isygoit.plan.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.application.port.LogProducer;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.LogBucket;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.exception.InfeasibleException;
import eu.isygoit.plan.exception.InvalidRequestException;
import eu.isygoit.plan.port.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class JobProcessor {
    private final PlanningService planningService;
    private final JobRepository jobRepository;
    private final JobStatusCache statusCache;
    private final LogProducer logProducer;
    private final ObjectMapper objectMapper;

    public JobProcessor(
            PlanningService planningService,
            JobRepository jobRepository,
            JobStatusCache statusCache,
            LogProducer logProducer,
            ObjectMapper objectMapper) {
        this.planningService = planningService;
        this.jobRepository = jobRepository;
        this.statusCache = statusCache;
        this.logProducer = logProducer;
        this.objectMapper = objectMapper;
    }

    public void process(PlanningRequest request) {
        var jobId = request.meta().jobId();
        var current = jobRepository.findStatus(jobId);
        if (current.isPresent() && isTerminal(current.get().status())) {
            return;
        }

        Instant startedAt = Instant.now();
        Instant submittedAt = current.map(JobStatus::submittedAt).orElse(startedAt);
        JobStatus normalizing = new JobStatus(jobId, IEnumJobStatusCode.Types.NORMALIZING,
                submittedAt, startedAt, null, null, "Normalizing request");
        if (!jobRepository.claimForProcessing(jobId, startedAt)) {
            jobRepository.findStatus(jobId).ifPresent(statusCache::put);
            return;
        }
        statusCache.put(normalizing);
        JobStatus solving = new JobStatus(jobId, IEnumJobStatusCode.Types.SOLVING,
                submittedAt, startedAt, null, null, "Solving");
        jobRepository.updateStatus(solving);
        statusCache.put(solving);

        SolveResult result;
        try {
            result = planningService.solve(request);
        } catch (InfeasibleException e) {
            String diagnostics = serialize(e.diagnostics());
            jobRepository.markInfeasible(jobId, e.getMessage(), diagnostics);
            JobStatus status = new JobStatus(jobId, IEnumJobStatusCode.Types.INFEASIBLE,
                    submittedAt, startedAt,
                    Instant.now(), "INFEASIBLE", e.getMessage());
            statusCache.put(status);
            publishLog(request, IEnumLogLevel.Types.WARN, "SOLVE", e.getMessage(), elapsedMillis(startedAt));
            return;
        } catch (InvalidRequestException e) {
            jobRepository.markFailed(jobId, e.code(), e.getMessage());
            JobStatus status = new JobStatus(jobId, IEnumJobStatusCode.Types.FAILED,
                    submittedAt, startedAt,
                    Instant.now(), e.code(), e.getMessage());
            statusCache.put(status);
            publishLog(request, IEnumLogLevel.Types.ERROR, "VALIDATE", e.getMessage(), elapsedMillis(startedAt));
            return;
        } catch (RuntimeException e) {
            log.error("Unexpected solver failure for planning job {}", jobId, e);
            String message = "Planning job failed unexpectedly";
            jobRepository.markFailed(jobId, "JOB_PROCESSING_FAILED", message);
            JobStatus status = new JobStatus(jobId, IEnumJobStatusCode.Types.FAILED,
                    submittedAt, startedAt, Instant.now(), "JOB_PROCESSING_FAILED", message);
            statusCache.put(status);
            publishLog(request, IEnumLogLevel.Types.ERROR, "SOLVE", message, elapsedMillis(startedAt));
            return;
        }

        jobRepository.upsert(jobId, result);
        IEnumJobStatusCode.Types status = result.status() == IEnumSolverStatus.Types.UNKNOWN
                ? IEnumJobStatusCode.Types.FAILED
                : IEnumJobStatusCode.Types.SUCCEEDED;
        JobStatus complete = new JobStatus(jobId, status,
                submittedAt, startedAt,
                Instant.now(), status == IEnumJobStatusCode.Types.FAILED ? "SOLVER_UNKNOWN" : null,
                result.status().name());
        jobRepository.updateStatus(complete);
        statusCache.put(complete);
        publishLog(request, IEnumLogLevel.Types.INFO, "SOLVE", result.status().name(), elapsedMillis(startedAt));
    }

    private void publishLog(PlanningRequest request, IEnumLogLevel.Types level, String step, String message, long duration) {
        Instant now = Instant.now();
        Instant bucket = LogBucket.start(now);
        logProducer.publishAsync(new LogEntry(request.meta().jobId(), request.tenant(), bucket, now,
                java.util.UUID.randomUUID(), level, step,
                request.solverOptions() == null ? "cp_sat" : request.solverOptions().engine(),
                message, null, duration, null, null));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize planning diagnostics", e);
        }
    }

    private long elapsedMillis(Instant startedAt) {
        return ChronoUnit.MILLIS.between(startedAt, Instant.now());
    }

    private boolean isTerminal(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.SUCCEEDED
                || status == IEnumJobStatusCode.Types.INFEASIBLE
                || status == IEnumJobStatusCode.Types.FAILED
                || status == IEnumJobStatusCode.Types.CANCELLED;
    }
}

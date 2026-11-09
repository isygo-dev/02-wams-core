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
import java.util.UUID;

@Service
@Slf4j
public class JobProcessor {
    private final PlanningService planningService;
    private final JobRepository jobRepository;
    private final JobStatusCache statusCache;
    private final LogProducer logProducer;
    private final ObjectMapper objectMapper;
    private final JobCancellationRegistry cancellationRegistry;

    public JobProcessor(
            PlanningService planningService,
            JobRepository jobRepository,
            JobStatusCache statusCache,
            LogProducer logProducer,
            ObjectMapper objectMapper,
            JobCancellationRegistry cancellationRegistry) {
        this.planningService = planningService;
        this.jobRepository = jobRepository;
        this.statusCache = statusCache;
        this.logProducer = logProducer;
        this.objectMapper = objectMapper;
        this.cancellationRegistry = cancellationRegistry;
    }

    public void process(PlanningRequest request) {
        var jobId = request.meta().jobId();
        var current = jobRepository.findStatus(jobId);
        if (current.isEmpty() || isTerminal(current.get().status())) {
            return;
        }

        Instant startedAt = Instant.now();
        Instant submittedAt = current.get().submittedAt();
        JobStatus normalizing = new JobStatus(jobId, IEnumJobStatusCode.Types.NORMALIZING,
                submittedAt, startedAt, null, null, "Normalizing request");
        if (!jobRepository.claimForProcessing(jobId, startedAt)) {
            jobRepository.findStatus(jobId).ifPresent(statusCache::put);
            return;
        }
        statusCache.put(normalizing);
        try (JobCancellationRegistry.Registration registration = cancellationRegistry.register(jobId)) {
            PlanningRequest canonicalRequest = jobRepository.findRequest(jobId).orElse(request);
            JobStatus solving = new JobStatus(jobId, IEnumJobStatusCode.Types.SOLVING,
                    submittedAt, startedAt, null, null, "Solving");
            if (!jobRepository.markSolving(jobId, solving)) {
                finishStopOrThrow(jobId, submittedAt, startedAt);
                return;
            }
            statusCache.put(solving);

            SolveResult result;
            try {
                result = planningService.solve(canonicalRequest, registration.token());
            } catch (InfeasibleException exception) {
                String diagnostics = serialize(exception.diagnostics());
                if (!jobRepository.markInfeasibleIfSolving(
                        jobId, exception.getMessage(), diagnostics)) {
                    finishStopOrThrow(jobId, submittedAt, startedAt);
                    return;
                }
                JobStatus status = new JobStatus(jobId, IEnumJobStatusCode.Types.INFEASIBLE,
                        submittedAt, startedAt, Instant.now(), "INFEASIBLE", exception.getMessage());
                statusCache.put(status);
                publishLog(canonicalRequest, IEnumLogLevel.Types.WARN,
                        "SOLVE", exception.getMessage(), elapsedMillis(startedAt));
                return;
            } catch (InvalidRequestException exception) {
                if (!jobRepository.markFailedIfSolving(jobId, exception.code(), exception.getMessage())) {
                    finishStopOrThrow(jobId, submittedAt, startedAt);
                    return;
                }
                JobStatus status = new JobStatus(jobId, IEnumJobStatusCode.Types.FAILED,
                        submittedAt, startedAt, Instant.now(), exception.code(), exception.getMessage());
                statusCache.put(status);
                publishLog(canonicalRequest, IEnumLogLevel.Types.ERROR,
                        "VALIDATE", exception.getMessage(), elapsedMillis(startedAt));
                return;
            } catch (RuntimeException exception) {
                log.error("Unexpected solver failure for planning job {}", jobId, exception);
                String message = "Planning job failed unexpectedly";
                if (!jobRepository.markFailedIfSolving(jobId, "JOB_PROCESSING_FAILED", message)) {
                    finishStopOrThrow(jobId, submittedAt, startedAt);
                    return;
                }
                JobStatus status = new JobStatus(jobId, IEnumJobStatusCode.Types.FAILED,
                        submittedAt, startedAt, Instant.now(), "JOB_PROCESSING_FAILED", message);
                statusCache.put(status);
                publishLog(canonicalRequest, IEnumLogLevel.Types.ERROR,
                        "SOLVE", message, elapsedMillis(startedAt));
                return;
            }

            IEnumJobStatusCode.Types finalStatus = result.status() == IEnumSolverStatus.Types.UNKNOWN
                    ? IEnumJobStatusCode.Types.FAILED
                    : IEnumJobStatusCode.Types.SUCCEEDED;
            JobStatus complete = new JobStatus(jobId, finalStatus,
                    submittedAt, startedAt, Instant.now(),
                    finalStatus == IEnumJobStatusCode.Types.FAILED ? "SOLVER_UNKNOWN" : null,
                    result.status().name());
            if (!jobRepository.completeSolving(jobId, complete, result)) {
                finishStopOrThrow(jobId, submittedAt, startedAt);
                return;
            }
            statusCache.put(complete);
            publishLog(canonicalRequest, IEnumLogLevel.Types.INFO,
                    "SOLVE", result.status().name(), elapsedMillis(startedAt));
        }
    }

    private void finishStopOrThrow(UUID jobId, Instant submittedAt, Instant startedAt) {
        if (jobRepository.completeStopped(jobId)) {
            statusCache.put(new JobStatus(jobId, IEnumJobStatusCode.Types.CANCELLED,
                    submittedAt, startedAt, Instant.now(), null, "Cancelled"));
            return;
        }
        JobStatus current = jobRepository.findStatus(jobId)
                .orElseThrow(() -> new IllegalStateException(
                        "Planning job disappeared while processing: " + jobId));
        statusCache.put(current);
        if (isTerminal(current.status())) {
            return;
        }
        throw new IllegalStateException(
                "Could not complete the planning job after its state changed to " + current.status());
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

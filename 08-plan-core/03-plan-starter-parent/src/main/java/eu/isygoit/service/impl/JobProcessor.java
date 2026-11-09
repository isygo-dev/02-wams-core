package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.async.kafka.PlanLogKafkaProducer;
import eu.isygoit.dto.data.LogBucket;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InfeasibleException;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.SolverBusyException;
import eu.isygoit.repository.PlanJobRepository;
import eu.isygoit.repository.PlanJobStatusView;
import eu.isygoit.service.impl.planning.PlanningService;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolveProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * Worker side of the job lifecycle: loads a persisted job by id, claims it (CREATED -> NORMALIZING), solves it and
 * persists the terminal state. Called by the Kafka job consumer and by the local executor
 * ({@link JobDispatchService}).
 * <p>
 * Every state transition is one native statement of {@link PlanJobRepository} run in its own short transaction
 * (never one transaction around the solve). Phase 0/1 behaviour is kept: claim, stop handling; SolverBusy and
 * shutdown release the claim instead of failing the job.
 * <p>
 * Engine v2 additions: the solve gets a {@link SolveListener} that publishes throttled {@code PROGRESS} log entries
 * (best-effort: a log failure never reaches the solver), and a {@code STOPPED_FEASIBLE} result (a stop request while
 * the engine held an incumbent) is persisted while the job still ends {@code CANCELLED}. An INFEASIBLE result reaches
 * the processor as an {@link InfeasibleException} (job INFEASIBLE with its diagnostics), unchanged.
 */
@Service
@Slf4j
public class JobProcessor {
    private final PlanningService planningService;
    private final PlanJobRepository jobRepository;
    private final PlanLogKafkaProducer logProducer;
    private final ObjectMapper objectMapper;
    private final JobCancellationRegistry cancellationRegistry;
    private final TransactionTemplate transactionTemplate;

    /** Minimum interval between two PROGRESS log entries of one job (the engines already throttle to about 1 s). */
    static final long PROGRESS_MIN_INTERVAL_NANOS = 1_000_000_000L;

    /**
     * Creates the processor.
     */
    public JobProcessor(
            PlanningService planningService,
            PlanJobRepository jobRepository,
            PlanLogKafkaProducer logProducer,
            ObjectMapper objectMapper,
            JobCancellationRegistry cancellationRegistry,
            PlatformTransactionManager transactionManager) {
        this.planningService = planningService;
        this.jobRepository = jobRepository;
        this.logProducer = logProducer;
        this.objectMapper = objectMapper;
        this.cancellationRegistry = cancellationRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Processes the job: no-op when the instance is shutting down, the job is unknown or already terminal, or
     * another worker owns the claim. Returns once the job reached a terminal state or its claim was released.
     *
     * @param jobId the database id of the plan job
     */
    public void process(Long jobId) {
        if (cancellationRegistry.isShuttingDown()) {
            // Leave the job CREATED: the dispatch recovery sweep of a live instance re-publishes it.
            log.info("Not starting planning job {}: instance is shutting down", jobId);
            return;
        }
        PlanJobStatusView current = jobRepository.findStatusViewById(jobId).orElse(null);
        if (current == null || isTerminal(current.getStatus())) {
            return;
        }
        String tenant = current.getTenant();
        String engine = current.getEngine();

        Instant startedAt = Instant.now();
        if (!update(() -> jobRepository.claimForProcessing(jobId, startedAt))) {
            return;
        }
        try (JobCancellationRegistry.Registration registration = cancellationRegistry.register(jobId)) {
            PlanningRequest request;
            try {
                request = loadRequest(jobId);
            } catch (RuntimeException exception) {
                log.error("Could not load the request of planning job {}", jobId, exception);
                request = null;
            }
            if (request == null) {
                String message = "The stored planning request is missing or unreadable";
                update(() -> jobRepository.failIfNotTerminal(jobId, "REQUEST_UNAVAILABLE", message));
                publishLog(jobId, tenant, engine, IEnumLogLevel.Types.ERROR, "LOAD", message, elapsedMillis(startedAt));
                return;
            }
            if (!update(() -> jobRepository.markSolving(jobId, "Solving"))) {
                finishStopOrThrow(jobId);
                return;
            }

            SolveResult result;
            ProgressLogger progress = new ProgressLogger(jobId, tenant, engine);
            try {
                result = planningService.solve(jobId, request, registration.token(), progress);
            } catch (InfeasibleException exception) {
                String diagnostics = serialize(exception.diagnostics());
                if (!update(() -> jobRepository.markInfeasibleIfSolving(jobId, exception.getMessage(), diagnostics))) {
                    finishStopOrThrow(jobId);
                    return;
                }
                publishLog(jobId, tenant, engine, IEnumLogLevel.Types.WARN,
                        "SOLVE", exception.getMessage(), elapsedMillis(startedAt));
                return;
            } catch (InvalidRequestException exception) {
                if (!update(() -> jobRepository.markFailedIfSolving(jobId, exception.code(), exception.getMessage()))) {
                    finishStopOrThrow(jobId);
                    return;
                }
                publishLog(jobId, tenant, engine, IEnumLogLevel.Types.ERROR,
                        "VALIDATE", exception.getMessage(), elapsedMillis(startedAt));
                return;
            } catch (SolverBusyException exception) {
                log.info("Solver busy for planning job {}: {}", jobId, exception.getMessage());
                releaseForRedispatch(jobId, "Solver busy; the job will be dispatched again");
                return;
            } catch (RuntimeException exception) {
                if (cancellationRegistry.isShuttingDown()) {
                    releaseForRedispatch(jobId, "Released on instance shutdown; the job will be dispatched again");
                    return;
                }
                log.error("Unexpected solver failure for planning job {}", jobId, exception);
                String message = "Planning job failed unexpectedly";
                if (!update(() -> jobRepository.markFailedIfSolving(jobId, "JOB_PROCESSING_FAILED", message))) {
                    finishStopOrThrow(jobId);
                    return;
                }
                publishLog(jobId, tenant, engine, IEnumLogLevel.Types.ERROR,
                        "SOLVE", message, elapsedMillis(startedAt));
                return;
            }

            if (cancellationRegistry.isShuttingDown() && registration.token().isCancellationRequested()) {
                // Interrupted by the shutdown: the (possibly partial) result is discarded and the job is
                // given back so another instance solves it from scratch.
                releaseForRedispatch(jobId, "Released on instance shutdown; the job will be dispatched again");
                return;
            }
            if (result.status() == IEnumSolverStatus.Types.STOPPED_FEASIBLE) {
                // A stop request met an incumbent: keep it. The job still ends CANCELLED (state machine untouched);
                // the status name goes to the message column like every other result status.
                String stoppedJson = serialize(result);
                String stoppedStatus = result.status().name();
                Instant stoppedAt = Instant.now();
                if (!update(() -> jobRepository.completeStoppedWithResult(jobId, stoppedJson, stoppedAt, stoppedStatus))) {
                    // The stop was already completed meanwhile (expired STOPPING sweep): same fallback as every path.
                    finishStopOrThrow(jobId);
                    return;
                }
                publishLog(jobId, tenant, engine, IEnumLogLevel.Types.INFO,
                        "SOLVE", stoppedStatus + progress.summarySuffix(), elapsedMillis(startedAt));
                return;
            }
            IEnumJobStatusCode.Types finalStatus = result.status() == IEnumSolverStatus.Types.UNKNOWN
                    ? IEnumJobStatusCode.Types.FAILED
                    : IEnumJobStatusCode.Types.SUCCEEDED;
            String errorCode = finalStatus == IEnumJobStatusCode.Types.FAILED ? "SOLVER_UNKNOWN" : null;
            String resultJson = serialize(result);
            String resultStatus = result.status().name();
            Instant finishedAt = Instant.now();
            if (!update(() -> jobRepository.completeSolving(
                    jobId, finalStatus.name(), resultJson, finishedAt, errorCode, resultStatus))) {
                finishStopOrThrow(jobId);
                return;
            }
            publishLog(jobId, tenant, engine, IEnumLogLevel.Types.INFO,
                    "SOLVE", result.status().name(), elapsedMillis(startedAt));
        }
    }

    /**
     * Throttled progress publisher handed to the engine: at most one {@code PROGRESS} INFO entry per
     * {@link #PROGRESS_MIN_INTERVAL_NANOS}, every event is remembered so the final log line can carry the last
     * objective / bound / solution count. Never throws and never blocks on the log pipeline (the producer is
     * fire-and-forget), so a solver callback thread is never delayed or broken by logging.
     */
    final class ProgressLogger implements SolveListener {
        private final Long jobId;
        private final String tenant;
        private final String engine;
        private long lastPublishedNanos;
        private boolean published;
        private volatile SolveProgress latest;

        ProgressLogger(Long jobId, String tenant, String engine) {
            this.jobId = jobId;
            this.tenant = tenant;
            this.engine = engine;
        }

        @Override
        public void onProgress(SolveProgress progress) {
            if (progress == null) {
                return;
            }
            latest = progress;
            try {
                long now = System.nanoTime();
                synchronized (this) {
                    if (published && now - lastPublishedNanos < PROGRESS_MIN_INTERVAL_NANOS) {
                        return;
                    }
                    published = true;
                    lastPublishedNanos = now;
                }
                publishLog(jobId, tenant, engine, IEnumLogLevel.Types.INFO, "PROGRESS",
                        describe(progress), progress.elapsedMillis());
            } catch (RuntimeException exception) {
                log.debug("Progress log of planning job {} dropped", jobId, exception);
            }
        }

        /**
         * @return {@code " (last: ...)"} describing the last progress event, or an empty string when none arrived
         */
        String summarySuffix() {
            SolveProgress last = latest;
            return last == null ? "" : " (last: " + describe(last) + ")";
        }

        /**
         * @return the latest progress event, or {@code null} when the engine reported none
         */
        SolveProgress latest() {
            return latest;
        }
    }

    /**
     * Message of a progress entry: {@code phase=SEARCH tier=0 solutions=3 objective=120.0 bound=100.0 gap=16.67%}
     * ({@code n/a} for unknown values; locale independent).
     */
    static String describe(SolveProgress progress) {
        return String.format(Locale.ROOT, "phase=%s tier=%d solutions=%d objective=%s bound=%s gap=%s",
                progress.phase() == null ? "n/a" : progress.phase(),
                progress.tier(),
                progress.solutionCount(),
                number(progress.objective()),
                number(progress.bestBound()),
                progress.gap() == null ? "n/a" : String.format(Locale.ROOT, "%.2f%%", progress.gap() * 100.0));
    }

    private static String number(Double value) {
        return value == null ? "n/a" : String.valueOf(value);
    }

    private PlanningRequest loadRequest(Long jobId) {
        String json = jobRepository.findRequestJson(jobId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PlanningRequest.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize the persisted planning request", exception);
        }
    }

    /**
     * Runs one modifying repository statement in its own transaction.
     *
     * @return true when at least one row was changed
     */
    private boolean update(IntSupplier statement) {
        Boolean changed = transactionTemplate.execute(status -> statement.getAsInt() > 0);
        return Boolean.TRUE.equals(changed);
    }

    /**
     * Gives the claim back (NORMALIZING/SOLVING -> CREATED, dispatch attempts unchanged). When the state moved on
     * meanwhile (a user stop), the regular stop completion applies.
     */
    private void releaseForRedispatch(Long jobId, String message) {
        if (update(() -> jobRepository.releaseClaim(jobId, message))) {
            log.info("Released the claim of planning job {}: {}", jobId, message);
            return;
        }
        finishStopOrThrow(jobId);
    }

    private void finishStopOrThrow(Long jobId) {
        if (update(() -> jobRepository.completeStopped(jobId))) {
            return;
        }
        PlanJobStatusView current = jobRepository.findStatusViewById(jobId)
                .orElseThrow(() -> new IllegalStateException(
                        "Planning job disappeared while processing: " + jobId));
        if (isTerminal(current.getStatus())) {
            return;
        }
        throw new IllegalStateException(
                "Could not complete the planning job after its state changed to " + current.getStatus());
    }

    private void publishLog(Long jobId, String tenant, String engine, IEnumLogLevel.Types level, String step,
                            String message, long duration) {
        Instant now = Instant.now();
        Instant bucket = LogBucket.start(now);
        logProducer.publish(new LogEntry(jobId, tenant, bucket, now,
                UUID.randomUUID(), level, step,
                engine == null ? SolverOptions.DEFAULT_ENGINE : engine,
                message, null, duration, null, null));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the planning job payload", e);
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

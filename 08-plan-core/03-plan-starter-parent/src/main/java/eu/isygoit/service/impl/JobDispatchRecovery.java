package eu.isygoit.service.impl;

import eu.isygoit.repository.PlanJobRepository;
import eu.isygoit.repository.PlanJobStatusView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * Periodic safety net of the job lifecycle (runs on the shared Spring scheduler pool).
 * <ul>
 *   <li>NORMALIZING/SOLVING jobs past the solver time limit plus grace -> FAILED (JOB_PROCESSING_TIMEOUT);</li>
 *   <li>STOPPING jobs whose worker never acknowledged the stop within the grace -> CANCELLED (the terminal state
 *       the stop flow normally ends in);</li>
 *   <li>CREATED jobs that used all their dispatch attempts -> FAILED (DISPATCH_FAILED);</li>
 *   <li>CREATED jobs not dispatched since the redispatch grace are claimed atomically in the database
 *       (one instance per job and interval) and dispatched again through {@link JobDispatchService}.</li>
 * </ul>
 * Each modifying statement of {@link PlanJobRepository} runs in its own short transaction.
 */
@Service
@Slf4j
public class JobDispatchRecovery {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final String STALE_JOB_ERROR_CODE = "JOB_PROCESSING_TIMEOUT";
    private static final String STALE_JOB_MESSAGE =
            "Processing exceeded the solver time limit plus the recovery grace period";
    static final String STALE_STOP_MESSAGE =
            "Stop completed by recovery: the worker did not acknowledge the stop within the grace period";
    static final String DISPATCH_FAILED_ERROR_CODE = "DISPATCH_FAILED";

    private final PlanJobRepository jobRepository;
    private final JobDispatchService dispatchService;
    private final JobCancellationRegistry cancellationRegistry;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int staleJobGraceSeconds;
    private final int redispatchGraceSeconds;
    private final int maxDispatchAttempts;
    private final AtomicBoolean sweeping = new AtomicBoolean();

    /**
     * Creates the recovery sweep.
     */
    public JobDispatchRecovery(
            PlanJobRepository jobRepository,
            JobDispatchService dispatchService,
            JobCancellationRegistry cancellationRegistry,
            PlatformTransactionManager transactionManager,
            @Value("${app.plan.execution.recovery-batch-size:100}") int batchSize,
            @Value("${app.plan.execution.stale-job-grace-seconds:240}") int staleJobGraceSeconds,
            @Value("${app.plan.execution.redispatch-grace-seconds:300}") int redispatchGraceSeconds,
            @Value("${app.plan.execution.max-dispatch-attempts:5}") int maxDispatchAttempts) {
        this.jobRepository = jobRepository;
        this.dispatchService = dispatchService;
        this.cancellationRegistry = cancellationRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("recovery batch size must be between 1 and " + MAX_BATCH_SIZE);
        }
        if (staleJobGraceSeconds < 0) {
            throw new IllegalArgumentException("stale job grace period must not be negative");
        }
        if (redispatchGraceSeconds < 0) {
            throw new IllegalArgumentException("redispatch grace period must not be negative");
        }
        if (maxDispatchAttempts < 1) {
            throw new IllegalArgumentException("max dispatch attempts must be at least 1");
        }
        this.batchSize = batchSize;
        this.staleJobGraceSeconds = staleJobGraceSeconds;
        this.redispatchGraceSeconds = redispatchGraceSeconds;
        this.maxDispatchAttempts = maxDispatchAttempts;
    }

    /**
     * One recovery sweep; a sweep still running skips the next one.
     */
    @Scheduled(fixedDelayString = "${app.plan.execution.recovery-interval-ms:30000}")
    public void recoverCreatedJobs() {
        if (cancellationRegistry.isShuttingDown()) {
            return;
        }
        if (!sweeping.compareAndSet(false, true)) {
            log.debug("Previous recovery sweep still running; skipping this one");
            return;
        }
        try {
            runStep("expired processing jobs", this::failExpiredProcessingJobs);
            runStep("expired stopping jobs", this::cancelExpiredStoppingJobs);
            runStep("exhausted dispatches", this::failExhaustedDispatches);
            runStep("created job dispatch", this::redispatchCreatedJobs);
        } finally {
            sweeping.set(false);
        }
    }

    private void runStep(String name, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException exception) {
            log.error("Recovery step '{}' failed", name, exception);
        }
    }

    /**
     * Runs one modifying repository statement in its own transaction.
     */
    private boolean update(IntSupplier statement) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> statement.getAsInt() > 0));
    }

    private void failExpiredProcessingJobs() {
        for (Long jobId : jobRepository.findExpiredProcessingJobIds(
                staleJobGraceSeconds, PageRequest.of(0, batchSize))) {
            try {
                if (update(() -> jobRepository.failExpiredProcessing(
                        jobId, staleJobGraceSeconds, STALE_JOB_ERROR_CODE, STALE_JOB_MESSAGE))) {
                    log.warn("Marked planning job {} as failed after its solver time limit and {}s recovery grace",
                            jobId, staleJobGraceSeconds);
                }
            } catch (RuntimeException exception) {
                log.error("Could not mark expired planning job {} as failed", jobId, exception);
            }
        }
    }

    private void cancelExpiredStoppingJobs() {
        for (Long jobId : jobRepository.findExpiredStoppingJobIds(
                staleJobGraceSeconds, PageRequest.of(0, batchSize))) {
            try {
                if (update(() -> jobRepository.cancelExpiredStopping(
                        jobId, staleJobGraceSeconds, STALE_STOP_MESSAGE))) {
                    log.warn("Marked stopping planning job {} as cancelled: no worker acknowledged the stop "
                            + "within {}s", jobId, staleJobGraceSeconds);
                }
            } catch (RuntimeException exception) {
                log.error("Could not cancel expired stopping planning job {}", jobId, exception);
            }
        }
    }

    private void failExhaustedDispatches() {
        Instant staleBefore = Instant.now().minusSeconds(redispatchGraceSeconds);
        String message = "The job could not be dispatched after " + maxDispatchAttempts + " attempts";
        List<Long> failed = transactionTemplate.execute(status -> jobRepository.failExhaustedDispatch(
                staleBefore, maxDispatchAttempts, DISPATCH_FAILED_ERROR_CODE, message, batchSize));
        for (Long jobId : failed == null ? List.<Long>of() : failed) {
            log.warn("Marked planning job {} as failed: {} dispatch attempts exhausted", jobId, maxDispatchAttempts);
        }
    }

    private void redispatchCreatedJobs() {
        int limit = batchSize;
        if (!dispatchService.isKafkaEnabled()) {
            // Never claim more than the executor queue can take: a full queue defers to the next sweep.
            limit = Math.min(limit, dispatchService.localQueueRemainingCapacity());
            if (limit < 1) {
                log.debug("Local executor queue is full; deferring the redispatch of created jobs");
                return;
            }
        }
        Instant now = Instant.now();
        int claimLimit = limit;
        List<Long> claimed = transactionTemplate.execute(status -> jobRepository.claimForDispatch(
                now, now.minusSeconds(redispatchGraceSeconds), maxDispatchAttempts, claimLimit));
        if (claimed == null) {
            return;
        }
        for (int index = 0; index < claimed.size(); index++) {
            Long jobId = claimed.get(index);
            try {
                Optional<PlanJobStatusView> job = jobRepository.findStatusViewById(jobId);
                if (job.isEmpty()) {
                    releaseQuietly(jobId, false);
                    continue;
                }
                dispatchService.dispatch(jobId, job.get().getTenant());
            } catch (TaskRejectedException exception) {
                // Executor full: defer this and every remaining claimed job to the next sweep; a deferral
                // is not a failed attempt.
                log.warn("Local executor rejected planning job {}; deferring {} claimed job(s)",
                        jobId, claimed.size() - index);
                for (int rest = index; rest < claimed.size(); rest++) {
                    releaseQuietly(claimed.get(rest), true);
                }
                return;
            } catch (RuntimeException exception) {
                log.error("Could not recover dispatch for planning job {}; it is retried after the grace period",
                        jobId, exception);
                releaseQuietly(jobId, false);
            }
        }
    }

    private void releaseQuietly(Long jobId, boolean refundAttempt) {
        try {
            update(() -> jobRepository.releaseDispatchClaim(jobId, refundAttempt));
        } catch (RuntimeException exception) {
            log.error("Could not release the dispatch claim of planning job {}", jobId, exception);
        }
    }
}

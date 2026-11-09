package eu.isygoit.service.impl;

import eu.isygoit.repository.PlanJobRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Graceful shutdown of the job workers of this instance. Stops first (highest phase), before the consumers and the
 * executors are torn down:
 * <ol>
 *   <li>no new job is started (registry flag; recovery sweeps and {@link JobProcessor} honour it);</li>
 *   <li>every running solve is asked to stop; workers that return release their claim themselves;</li>
 *   <li>waits up to {@code app.plan.execution.shutdown-wait-seconds} (keep it below
 *       {@code spring.lifecycle.timeout-per-shutdown-phase}) for the solves to return;</li>
 *   <li>the claims of jobs still running after the wait are released (NORMALIZING/SOLVING -> CREATED,
 *       {@code dispatched_at} cleared) so that another instance re-claims them through the dispatch recovery
 *       instead of waiting for the stale sweep. Claims carry no owner: only job ids registered on this very
 *       instance are released.</li>
 * </ol>
 */
@Component
@Slf4j
public class JobWorkerShutdown implements SmartLifecycle {
    static final String RELEASE_MESSAGE = "Released on instance shutdown; the job will be dispatched again";
    private static final long POLL_MILLIS = 100;

    private final JobCancellationRegistry cancellationRegistry;
    private final PlanJobRepository jobRepository;
    private final TransactionTemplate transactionTemplate;
    private final int shutdownWaitSeconds;
    private volatile boolean running;

    public JobWorkerShutdown(
            JobCancellationRegistry cancellationRegistry,
            PlanJobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Value("${app.plan.execution.shutdown-wait-seconds:20}") int shutdownWaitSeconds) {
        if (shutdownWaitSeconds < 0) {
            throw new IllegalArgumentException("shutdown wait must not be negative");
        }
        this.cancellationRegistry = cancellationRegistry;
        this.jobRepository = jobRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.shutdownWaitSeconds = shutdownWaitSeconds;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        shutdownWorkers();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    void shutdownWorkers() {
        cancellationRegistry.beginShutdown();
        cancellationRegistry.cancelAll();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(shutdownWaitSeconds);
        while (cancellationRegistry.hasActive() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        for (Long jobId : cancellationRegistry.activeJobIds()) {
            try {
                if (Boolean.TRUE.equals(transactionTemplate.execute(
                        status -> jobRepository.releaseClaim(jobId, RELEASE_MESSAGE) > 0))) {
                    log.info("Released the claim of planning job {} still running at shutdown", jobId);
                }
            } catch (RuntimeException exception) {
                log.error("Could not release the claim of planning job {} at shutdown", jobId, exception);
            }
        }
    }
}

package eu.isygoit.service.impl.planning;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.exception.SolverBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Node-wide limit on the number of solves running at the same time (fair semaphore). A solve waits a bounded time for
 * a slot, stops waiting as soon as its cancellation token is requested and always releases the slot in
 * {@link Permit#close()}.
 */
@Slf4j
@Component
public class SolveConcurrencyGuard {
    private static final long POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(250);

    private final Semaphore semaphore;
    private final int maxPermits;
    private final Duration timeout;

    @Autowired
    public SolveConcurrencyGuard(PlanLimitsProperties limits) {
        this(limits.resolveMaxConcurrentSolves(), Duration.ofSeconds(limits.getSolveQueueTimeoutSeconds()));
    }

    public SolveConcurrencyGuard(int maxPermits, Duration timeout) {
        this.maxPermits = Math.max(1, maxPermits);
        this.timeout = timeout;
        this.semaphore = new Semaphore(this.maxPermits, true);
    }

    /**
     * Acquires a solver slot.
     *
     * @param token cancellation token of the job; waiting stops when it is requested
     * @return the permit, to be closed in a finally block
     * @throws SolverBusyException     when no slot became free within the configured timeout
     * @throws SolveCancelledException when the token was cancelled or the thread interrupted while waiting
     */
    public Permit acquire(SolverCancellationToken token) {
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (true) {
                if (token.isCancellationRequested()) {
                    throw new SolveCancelledException(SolveCancelledException.STOP_REQUESTED);
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    log.warn("No solver slot free within {}s (max concurrent solves {})",
                            timeout.toSeconds(), maxPermits);
                    throw new SolverBusyException(maxPermits, timeout.toSeconds());
                }
                if (semaphore.tryAcquire(Math.min(remaining, POLL_NANOS), TimeUnit.NANOSECONDS)) {
                    if (token.isCancellationRequested()) {
                        semaphore.release();
                        throw new SolveCancelledException(SolveCancelledException.STOP_REQUESTED);
                    }
                    return new Permit(semaphore);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SolveCancelledException(SolveCancelledException.STOP_REQUESTED);
        }
    }

    /**
     * Gets the configured number of slots.
     *
     * @return the slot count
     */
    public int maxPermits() {
        return maxPermits;
    }

    /**
     * Gets the free slots (metrics).
     *
     * @return the free slots
     */
    public int availablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * Gets the solves currently running (metrics).
     *
     * @return the running solves
     */
    public int activeSolves() {
        return maxPermits - semaphore.availablePermits();
    }

    /**
     * Gets the solves currently waiting for a slot (metrics).
     *
     * @return the waiting solves
     */
    public int queueLength() {
        return semaphore.getQueueLength();
    }

    /** A held solver slot; closing it more than once releases it only once. */
    public static final class Permit implements AutoCloseable {
        private final Semaphore semaphore;
        private final AtomicBoolean released = new AtomicBoolean();

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}

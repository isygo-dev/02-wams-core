package eu.isygoit.service.impl.planning;

import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.exception.SolverBusyException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolveConcurrencyGuardTest {

    @Test
    void acquiresAndReleasesPermits() {
        SolveConcurrencyGuard guard = new SolveConcurrencyGuard(2, Duration.ofSeconds(1));
        SolveConcurrencyGuard.Permit first = guard.acquire(new SolverCancellationToken());
        SolveConcurrencyGuard.Permit second = guard.acquire(new SolverCancellationToken());
        assertEquals(0, guard.availablePermits());
        assertEquals(2, guard.activeSolves());

        first.close();
        first.close(); // releasing twice must not create an extra permit
        assertEquals(1, guard.availablePermits());
        second.close();
        assertEquals(2, guard.availablePermits());
        assertEquals(2, guard.maxPermits());
    }

    @Test
    void failsWithSolverBusyWhenNoSlotBecomesFree() {
        SolveConcurrencyGuard guard = new SolveConcurrencyGuard(1, Duration.ofMillis(300));
        try (SolveConcurrencyGuard.Permit ignored = guard.acquire(new SolverCancellationToken())) {
            SolverBusyException exception = assertThrows(SolverBusyException.class,
                    () -> guard.acquire(new SolverCancellationToken()));
            assertEquals(SolverBusyException.CODE, exception.code());
        }
        assertEquals(1, guard.availablePermits());
    }

    @Test
    void waitingSolveStopsWhenTokenIsCancelled() throws Exception {
        SolveConcurrencyGuard guard = new SolveConcurrencyGuard(1, Duration.ofSeconds(30));
        SolverCancellationToken waiting = new SolverCancellationToken();
        try (SolveConcurrencyGuard.Permit ignored = guard.acquire(new SolverCancellationToken())) {
            CompletableFuture<Throwable> outcome = CompletableFuture.supplyAsync(() -> {
                try {
                    guard.acquire(waiting).close();
                    return null;
                } catch (Throwable failure) {
                    return failure;
                }
            });
            Thread.sleep(300);
            waiting.cancel();
            Throwable failure = outcome.get(5, TimeUnit.SECONDS);
            assertTrue(failure instanceof SolveCancelledException, "waiter must stop on cancellation");
        }
        assertEquals(1, guard.availablePermits());
    }

    @Test
    void alreadyCancelledTokenNeverTakesAPermit() {
        SolveConcurrencyGuard guard = new SolveConcurrencyGuard(1, Duration.ofSeconds(1));
        SolverCancellationToken token = new SolverCancellationToken();
        token.cancel();
        assertThrows(SolveCancelledException.class, () -> guard.acquire(token));
        assertEquals(1, guard.availablePermits());
    }
}

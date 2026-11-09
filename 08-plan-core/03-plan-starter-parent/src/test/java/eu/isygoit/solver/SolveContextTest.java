package eu.isygoit.solver;

import eu.isygoit.dto.request.SolverOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolveContextTest {

    @Test
    void remainingTimeIsCappedByEngineAttemptAndWholeRequestBudgets() {
        SolverOptions attemptOptions = new SolverOptions(null, 20, null, null, null, null, null, null, null, null,
                null, null, null);
        long requestStart = 1_000_000_000L;
        long attemptStart = 3_000_000_000L;
        SolveContext attemptLimited = new SolveContext(attemptOptions, null, SolveListener.NOOP,
                requestStart, attemptStart, 5_000L);

        assertEquals(5_000L, attemptLimited.remainingMillis(attemptStart));
        assertEquals(3_000L, attemptLimited.remainingMillis(attemptStart + 2_000_000_000L));
        assertEquals(0L, attemptLimited.remainingMillis(attemptStart + 5_000_000_000L));

        SolverOptions requestOptions = new SolverOptions(null, 10, null, null, null, null, null, null, null, null,
                null, null, null);
        SolveContext requestLimited = new SolveContext(requestOptions, null, SolveListener.NOOP,
                requestStart, attemptStart, 9_000L);
        assertEquals(1_000L, requestLimited.remainingMillis(requestStart + 9_000_000_000L));
        assertEquals(0L, requestLimited.remainingMillis(requestStart + 10_000_000_000L));
    }
}

package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

/**
 * Thrown when no solver slot became free within {@code app.plan.limits.solve-queue-timeout-seconds}. The request
 * itself is valid: the failure is transient and the job should be retried, not failed permanently.
 */
@MsgLocale(value = "plan.error.solver.busy", httpStatus = HttpStatus.SERVICE_UNAVAILABLE)
public final class SolverBusyException extends ManagedException {
    public static final String CODE = "SOLVER_BUSY";

    public SolverBusyException(int permits, long timeoutSeconds) {
        super("All " + permits + " solver slots are busy; no slot became free within " + timeoutSeconds + "s");
    }

    public String code() {
        return CODE;
    }
}

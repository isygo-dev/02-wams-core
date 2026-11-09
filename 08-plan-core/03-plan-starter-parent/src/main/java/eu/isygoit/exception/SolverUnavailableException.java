package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the requested solver cannot run on this node (for example the OR-Tools native library failed to load).
 */
@MsgLocale(value = "plan.error.solver.unavailable", httpStatus = HttpStatus.SERVICE_UNAVAILABLE)
public final class SolverUnavailableException extends InvalidRequestException {
    public static final String CODE = "SOLVER_UNAVAILABLE";

    public SolverUnavailableException(String engine, Throwable cause) {
        super(CODE, "Solver '" + engine + "' is unavailable on this node: " + cause, cause);
    }
}

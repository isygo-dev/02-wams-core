package eu.isygoit.plan.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

@MsgLocale(value = "plan.error.invalid.solver.result", httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
public final class InvalidSolverResultException extends InvalidRequestException {
    public InvalidSolverResultException(String message) {
        super("INVALID_SOLVER_RESULT", message);
    }
}

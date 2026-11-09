package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

@MsgLocale(value = "plan.error.invalid.solver.result", httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
public final class InvalidSolverResultException extends InvalidRequestException {
    public InvalidSolverResultException(String message) {
        super("INVALID_SOLVER_RESULT", message);
    }

    /**
     * @param code    specific failure code (for example {@code SOLUTION_INVALID_OVERLAP}, {@code TIER_VALUE_MISMATCH})
     * @param message human readable detail
     */
    public InvalidSolverResultException(String code, String message) {
        super(code, message);
    }
}

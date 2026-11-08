package eu.isygoit.plan.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

import java.util.Collection;

@MsgLocale(value = "plan.error.unknown.solver", httpStatus = HttpStatus.BAD_REQUEST)
public final class UnknownSolverException extends InvalidRequestException {
    public UnknownSolverException(String engine, Collection<String> supported) {
        super("UNKNOWN_SOLVER", "Solver '" + engine + "' is not supported. Supported solvers: " + supported);
    }
}

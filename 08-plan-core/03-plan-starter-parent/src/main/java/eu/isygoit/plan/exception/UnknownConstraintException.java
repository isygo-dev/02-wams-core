package eu.isygoit.plan.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

import java.util.Collection;

@MsgLocale(value = "plan.error.unknown.constraint", httpStatus = HttpStatus.BAD_REQUEST)
public final class UnknownConstraintException extends InvalidRequestException {
    public UnknownConstraintException(String type, Collection<String> supported) {
        super("UNKNOWN_CONSTRAINT_TYPE",
                "Constraint type '" + type + "' is not supported. Supported types: " + supported);
    }
}

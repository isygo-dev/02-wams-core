package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

import java.util.Collection;

@MsgLocale(value = "plan.error.unknown.objective", httpStatus = HttpStatus.BAD_REQUEST)
public final class UnknownObjectiveException extends InvalidRequestException {
    public UnknownObjectiveException(String type, Collection<String> supported) {
        super("UNKNOWN_OBJECTIVE_TYPE",
                "Objective type '" + type + "' is not supported. Supported types: " + supported);
    }
}

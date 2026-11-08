package eu.isygoit.plan.exception;

import eu.isygoit.annotation.MsgLocale;
import eu.isygoit.exception.ManagedException;
import org.springframework.http.HttpStatus;

@MsgLocale(value = "plan.error.invalid.request", httpStatus = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends ManagedException {
    private final String code;

    public InvalidRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public InvalidRequestException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

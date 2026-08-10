package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.reason.exception")
public class InvalidReasonException extends ManagedException {

    public InvalidReasonException(String message) {
        super(message);
    }

    public InvalidReasonException(String message, Throwable cause) {
        super(message, cause);
    }
}

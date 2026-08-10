package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.auth.type.exception")
public class InvalidAuthTypeException extends ManagedException {

    public InvalidAuthTypeException(String message) {
        super(message);
    }

    public InvalidAuthTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}

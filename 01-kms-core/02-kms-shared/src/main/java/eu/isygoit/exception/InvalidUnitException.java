package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.unit.exception")
public class InvalidUnitException extends ManagedException {

    public InvalidUnitException(String message) {
        super(message);
    }

    public InvalidUnitException(String message, Throwable cause) {
        super(message, cause);
    }
}

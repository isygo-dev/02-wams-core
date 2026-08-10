package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("missing.required.field.exception")
public class MissingRequiredFieldException extends ManagedException {

    public MissingRequiredFieldException(String message) {
        super(message);
    }

    public MissingRequiredFieldException(String message, Throwable cause) {
        super(message, cause);
    }
}

package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("missing.keyid.exception")
public class MissingKeyIdException extends ManagedException {

    public MissingKeyIdException(String message) {
        super(message);
    }

    public MissingKeyIdException(String message, Throwable cause) {
        super(message, cause);
    }
}

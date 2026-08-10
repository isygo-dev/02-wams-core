package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.jwt.keyspec.exception")
public class InvalidJwtKeySpecException extends ManagedException {

    public InvalidJwtKeySpecException(String message) {
        super(message);
    }

    public InvalidJwtKeySpecException(String message, Throwable cause) {
        super(message, cause);
    }
}

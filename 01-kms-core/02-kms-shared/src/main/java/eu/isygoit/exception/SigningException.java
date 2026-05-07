package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

/**
 * The type Signing exception.
 */
@MsgLocale("tbd.exception")
public class SigningException extends RuntimeException {

    /**
     * Instantiates a new Signing exception.
     *
     * @param message the message
     */
    public SigningException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Signing exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }
}


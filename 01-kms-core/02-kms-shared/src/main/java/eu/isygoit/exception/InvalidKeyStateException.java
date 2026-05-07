package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

/**
 * The type Invalid key state exception.
 */
@MsgLocale("tbd.exception")
public class InvalidKeyStateException extends RuntimeException {

    /**
     * Instantiates a new Invalid key state exception.
     *
     * @param message the message
     */
    public InvalidKeyStateException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Invalid key state exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public InvalidKeyStateException(String message, Throwable cause) {
        super(message, cause);
    }
}


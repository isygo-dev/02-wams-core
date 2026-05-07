package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

/**
 * The type Encryption exception.
 */
@MsgLocale("tbd.exception")
public class EncryptionException extends RuntimeException {

    /**
     * Instantiates a new Encryption exception.
     *
     * @param message the message
     */
    public EncryptionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Encryption exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}


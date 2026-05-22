package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

/**
 * The type Decryption exception.
 */
@MsgLocale("decryption.exception")
public class DecryptionException extends RuntimeException {

    /**
     * Instantiates a new Decryption exception.
     *
     * @param message the message
     */
    public DecryptionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Decryption exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}


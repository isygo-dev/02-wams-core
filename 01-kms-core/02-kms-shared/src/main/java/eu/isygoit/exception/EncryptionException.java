package eu.isygoit.exception;

/**
 * The type Encryption exception.
 */
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


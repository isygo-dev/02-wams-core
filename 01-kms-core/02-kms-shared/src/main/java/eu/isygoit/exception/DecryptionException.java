package eu.isygoit.exception;

/**
 * The type Decryption exception.
 */
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


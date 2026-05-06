package eu.isygoit.exception;

/**
 * The type Key not found exception.
 */
public class KeyNotFoundException extends RuntimeException {

    /**
     * Instantiates a new Key not found exception.
     *
     * @param keyId the key id
     */
    public KeyNotFoundException(String keyId) {
        super("Key not found: " + keyId);
    }

    /**
     * Instantiates a new Key not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public KeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}


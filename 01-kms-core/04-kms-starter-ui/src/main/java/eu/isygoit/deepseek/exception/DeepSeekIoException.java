package eu.isygoit.deepseek.exception;

/**
 * Thrown for network errors, timeouts, or other I/O issues
 * (where no HTTP status code is available).
 */
public final class DeepSeekIoException extends DeepSeekException {
    public DeepSeekIoException(String message, Throwable cause) {
        super(message, cause, 0);  // 0 indicates no HTTP status
    }

    public DeepSeekIoException(String message) {
        super(message, 0);
    }
}
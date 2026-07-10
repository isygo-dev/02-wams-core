package eu.isygoit.deepseek.exception;

public sealed abstract class DeepSeekException extends Exception
        permits AuthenticationException,
        RateLimitException,
        InvalidRequestException,
        ApiServerException,
        DeepSeekIoException {

    private final int statusCode;

    protected DeepSeekException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    protected DeepSeekException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
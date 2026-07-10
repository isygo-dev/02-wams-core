package eu.isygoit.deepseek.exception;

public final class RateLimitException extends DeepSeekException {
    public RateLimitException(String message) {
        super(message, 429);
    }
}
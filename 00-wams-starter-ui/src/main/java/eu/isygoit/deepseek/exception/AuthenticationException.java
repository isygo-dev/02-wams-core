package eu.isygoit.deepseek.exception;

public final class AuthenticationException extends DeepSeekException {
    public AuthenticationException(String message) {
        super(message, 401);
    }
}
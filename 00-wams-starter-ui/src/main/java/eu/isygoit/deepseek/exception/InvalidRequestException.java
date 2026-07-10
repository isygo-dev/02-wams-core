package eu.isygoit.deepseek.exception;

public final class InvalidRequestException extends DeepSeekException {
    public InvalidRequestException(String message) {
        super(message, 400);
    }
}
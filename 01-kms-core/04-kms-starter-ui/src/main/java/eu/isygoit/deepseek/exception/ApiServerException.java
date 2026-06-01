package eu.isygoit.deepseek.exception;

public final class ApiServerException extends DeepSeekException {
    public ApiServerException(String message, int statusCode) {
        super(message, statusCode);
    }
}
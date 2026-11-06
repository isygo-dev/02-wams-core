package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;
import eu.isygoit.exception.ManagedException;

@MsgLocale("deepseek.exception")
public sealed abstract class DeepSeekException extends ManagedException
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
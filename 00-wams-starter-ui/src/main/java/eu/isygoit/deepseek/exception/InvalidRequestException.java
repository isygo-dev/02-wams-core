package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("deepseek.invalid.request.exception")
public final class InvalidRequestException extends DeepSeekException {
    public InvalidRequestException(String message) {
        super(message, 400);
    }
}
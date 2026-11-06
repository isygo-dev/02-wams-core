package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("deepseek.api.server.exception")
public final class ApiServerException extends DeepSeekException {
    public ApiServerException(String message, int statusCode) {
        super(message, statusCode);
    }
}
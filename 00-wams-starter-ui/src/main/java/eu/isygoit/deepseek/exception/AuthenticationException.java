package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("deepseek.authentication.exception")
public final class AuthenticationException extends DeepSeekException {
    public AuthenticationException(String message) {
        super(message, 401);
    }
}
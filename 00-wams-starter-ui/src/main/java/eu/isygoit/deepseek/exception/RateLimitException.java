package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("deepseek.rate.limit.exception")
public final class RateLimitException extends DeepSeekException {
    public RateLimitException(String message) {
        super(message, 429);
    }
}
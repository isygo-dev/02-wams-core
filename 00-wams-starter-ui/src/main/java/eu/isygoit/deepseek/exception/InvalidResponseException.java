package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.response.exception")
public class InvalidResponseException extends RuntimeException {

    public InvalidResponseException(String message) {
        super(message);
    }

    public InvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

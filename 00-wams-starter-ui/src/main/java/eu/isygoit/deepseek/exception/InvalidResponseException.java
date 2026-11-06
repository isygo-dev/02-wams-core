package eu.isygoit.deepseek.exception;

import eu.isygoit.annotation.MsgLocale;
import eu.isygoit.exception.ManagedException;

@MsgLocale("invalid.response.exception")
public class InvalidResponseException extends ManagedException {

    public InvalidResponseException(String message) {
        super(message);
    }

    public InvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

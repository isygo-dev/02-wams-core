package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.not.found.in.hsm.exception")
public class KeyNotFoundInHsmException extends ManagedException {

    public KeyNotFoundInHsmException(String message) {
        super(message);
    }

    public KeyNotFoundInHsmException(String message, Throwable cause) {
        super(message, cause);
    }
}

package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("private.key.not.found.exception")
public class PrivateKeyNotFoundException extends ManagedException {

    public PrivateKeyNotFoundException(String message) {
        super(message);
    }

    public PrivateKeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

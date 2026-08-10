package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("reencrypt.not.implemented.exception")
public class ReEncryptNotImplementedException extends ManagedException {

    public ReEncryptNotImplementedException(String message) {
        super(message);
    }

    public ReEncryptNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }
}

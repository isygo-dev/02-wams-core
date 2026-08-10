package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unknown.custom.keystore.status.exception")
public class UnknownCustomKeyStoreStatusException extends ManagedException {

    public UnknownCustomKeyStoreStatusException(String message) {
        super(message);
    }

    public UnknownCustomKeyStoreStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}

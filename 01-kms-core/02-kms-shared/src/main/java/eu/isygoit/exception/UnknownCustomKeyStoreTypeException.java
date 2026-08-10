package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unknown.custom.keystore.type.exception")
public class UnknownCustomKeyStoreTypeException extends ManagedException {

    public UnknownCustomKeyStoreTypeException(String message) {
        super(message);
    }

    public UnknownCustomKeyStoreTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}

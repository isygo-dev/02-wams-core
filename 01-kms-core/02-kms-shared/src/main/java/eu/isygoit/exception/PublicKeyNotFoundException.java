package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("public.key.not.found.exception")
public class PublicKeyNotFoundException extends ManagedException {

    public PublicKeyNotFoundException(String message) {
        super(message);
    }

    public PublicKeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

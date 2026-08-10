package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("asymmetric.key.missing.public.key.exception")
public class AsymmetricKeyMissingPublicKeyException extends ManagedException {

    public AsymmetricKeyMissingPublicKeyException(String message) {
        super(message);
    }

    public AsymmetricKeyMissingPublicKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}

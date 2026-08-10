package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unsupported.keypair.spec.exception")
public class UnsupportedKeyPairSpecException extends ManagedException {

    public UnsupportedKeyPairSpecException(String message) {
        super(message);
    }

    public UnsupportedKeyPairSpecException(String message, Throwable cause) {
        super(message, cause);
    }
}

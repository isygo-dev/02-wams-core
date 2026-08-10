package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unsupported.encryption.algorithm.exception")
public class UnsupportedEncryptionAlgorithmException extends ManagedException {

    public UnsupportedEncryptionAlgorithmException(String message) {
        super(message);
    }

    public UnsupportedEncryptionAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}

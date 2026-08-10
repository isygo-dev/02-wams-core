package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unsupported.signature.algorithm.exception")
public class UnsupportedSignatureAlgorithmException extends ManagedException {

    public UnsupportedSignatureAlgorithmException(String message) {
        super(message);
    }

    public UnsupportedSignatureAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}

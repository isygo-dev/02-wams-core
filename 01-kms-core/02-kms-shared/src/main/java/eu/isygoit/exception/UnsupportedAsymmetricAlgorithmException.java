package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unsupported.asymmetric.algorithm.exception")
public class UnsupportedAsymmetricAlgorithmException extends ManagedException {

    public UnsupportedAsymmetricAlgorithmException(String message) {
        super(message);
    }

    public UnsupportedAsymmetricAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}

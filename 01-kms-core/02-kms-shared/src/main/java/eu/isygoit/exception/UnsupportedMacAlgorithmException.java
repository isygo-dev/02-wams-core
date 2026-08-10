package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("unsupported.mac.algorithm.exception")
public class UnsupportedMacAlgorithmException extends ManagedException {

    public UnsupportedMacAlgorithmException(String message) {
        super(message);
    }

    public UnsupportedMacAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}

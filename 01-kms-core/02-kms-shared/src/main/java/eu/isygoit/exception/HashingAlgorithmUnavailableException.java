package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("hashing.algorithm.unavailable.exception")
public class HashingAlgorithmUnavailableException extends ManagedException {

    public HashingAlgorithmUnavailableException(String message) {
        super(message);
    }

    public HashingAlgorithmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

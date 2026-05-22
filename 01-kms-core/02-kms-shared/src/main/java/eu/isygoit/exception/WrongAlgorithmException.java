package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("wrong.algorithm.exception")
public class WrongAlgorithmException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public WrongAlgorithmException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public WrongAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}

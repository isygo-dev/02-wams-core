package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("illegal.key.origin.exception")
public class IllegalKeyOriginException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public IllegalKeyOriginException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public IllegalKeyOriginException(String message, Throwable cause) {
        super(message, cause);
    }
}

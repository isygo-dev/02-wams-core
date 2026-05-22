package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("disabled.key.exception")
public class DisabledKeyException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public DisabledKeyException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public DisabledKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}

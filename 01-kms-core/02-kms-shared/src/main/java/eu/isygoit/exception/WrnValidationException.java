package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("wrn.validation.exception")
public class WrnValidationException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public WrnValidationException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public WrnValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

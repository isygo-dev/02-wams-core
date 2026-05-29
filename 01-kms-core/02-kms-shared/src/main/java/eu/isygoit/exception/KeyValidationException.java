package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.validation.exception")
public class KeyValidationException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KeyValidationException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public KeyValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

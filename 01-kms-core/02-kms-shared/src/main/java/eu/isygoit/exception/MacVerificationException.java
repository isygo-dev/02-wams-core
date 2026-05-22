package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("mac.verification.exception")
public class MacVerificationException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public MacVerificationException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public MacVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

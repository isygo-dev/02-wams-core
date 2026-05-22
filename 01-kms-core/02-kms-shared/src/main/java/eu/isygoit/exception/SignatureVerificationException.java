package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("signature.verification.exception")
public class SignatureVerificationException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public SignatureVerificationException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public SignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.grant.exception")
public class KeyGrantException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KeyGrantException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public KeyGrantException(String message, Throwable cause) {
        super(message, cause);
    }
}

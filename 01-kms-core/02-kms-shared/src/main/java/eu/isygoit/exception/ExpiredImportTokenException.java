package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("expired.import.token.exception")
public class ExpiredImportTokenException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public ExpiredImportTokenException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ExpiredImportTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

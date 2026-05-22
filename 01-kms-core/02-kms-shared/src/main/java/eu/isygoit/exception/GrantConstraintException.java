package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("gran.tconstraint.exception")
public class GrantConstraintException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public GrantConstraintException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public GrantConstraintException(String message, Throwable cause) {
        super(message, cause);
    }
}

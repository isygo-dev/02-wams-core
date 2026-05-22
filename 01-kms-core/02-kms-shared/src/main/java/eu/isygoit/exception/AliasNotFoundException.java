package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("alias.not.found.exception")
public class AliasNotFoundException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public AliasNotFoundException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public AliasNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

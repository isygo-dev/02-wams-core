package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.alias.not.found.exception")
public class KeyAliasNotFoundException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KeyAliasNotFoundException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public KeyAliasNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

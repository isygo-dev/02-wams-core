package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("key.rotation.exception")
public class KeyRotationException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KeyRotationException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public KeyRotationException(String message, Throwable cause) {
        super(message, cause);
    }
}

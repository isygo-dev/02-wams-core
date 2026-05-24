package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("no.active.version.exception.exception")
public class NoActiveVersionException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public NoActiveVersionException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Missing xks proxy path exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public NoActiveVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
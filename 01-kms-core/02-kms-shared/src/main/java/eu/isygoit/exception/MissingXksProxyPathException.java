package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("MissingXksProxyPathException.exception")
public class MissingXksProxyPathException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingXksProxyPathException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Missing xks proxy path exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public MissingXksProxyPathException(String message, Throwable cause) {
        super(message, cause);
    }
}

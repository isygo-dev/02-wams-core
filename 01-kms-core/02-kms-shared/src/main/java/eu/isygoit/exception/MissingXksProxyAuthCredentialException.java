package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("missing.xks.proxy.auth.credential.exception.exception")
public class MissingXksProxyAuthCredentialException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingXksProxyAuthCredentialException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Missing xks proxy auth credential exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public MissingXksProxyAuthCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}




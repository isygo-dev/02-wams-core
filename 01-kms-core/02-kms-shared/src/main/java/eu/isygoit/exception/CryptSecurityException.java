package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("crypy.security.exception")
public class CryptSecurityException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CryptSecurityException(String s) {
        super(s);
    }

    public CryptSecurityException(String s, Throwable cause) {
        super(s, cause);
    }
}

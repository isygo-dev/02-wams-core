package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("crypy.bad.padding.exception")
public class CryptBadPaddingException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CryptBadPaddingException(String s) {
        super(s);
    }

    public CryptBadPaddingException(String s, Throwable cause) {
        super(s, cause);
    }
}

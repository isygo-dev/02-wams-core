package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("crypy.bad.tag.exception")
public class CryptBadTagException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CryptBadTagException(String s) {
        super(s);
    }

    public CryptBadTagException(String s, Throwable cause) {
        super(s, cause);
    }
}

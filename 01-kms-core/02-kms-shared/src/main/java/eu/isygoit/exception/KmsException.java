package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("KmsException.exception")
public class KmsException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KmsException(String s) {
        super(s);
    }

    public KmsException(String s, Throwable cause) {
        super(s, cause);
    }
}